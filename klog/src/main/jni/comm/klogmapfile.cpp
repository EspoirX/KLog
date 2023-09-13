//
// Created by Administrator on 2020/8/24.
//

#include <errno.h>
#include <unistd.h>
#include <sys/mman.h>
#include <exception>
#include <fcntl.h>
#include <sys/stat.h>
#include <comm/android/callstack.h>
#include <comm/xlogger/xlogger.h>

#include "klogmapfile.h"

//klogmapfile::klogmapfile() : handle_(-1), data_(nullptr), size_(0), error_(false){
//    //TODO
//}
klogmapfile::klogmapfile() {
    //TODO
}

klogmapfile::~klogmapfile() {
    close();
}

void klogmapfile::clear(bool error) {
    data_ = 0;
    size_ = 0;
    handle_ = -1;
    error_ = error;
}

char *klogmapfile::data() const {
    return data_;
}

bool klogmapfile::is_open() {
    return data_ != 0 && handle_ >= 0;
}

void klogmapfile::open(detail::mapped_file_params_base &p) {
    if (is_open()) {
        return;
    }
    p.normalize();
    open_file(p);
    map_file(p);  // May modify p.hint
    params_ = p;
}

void klogmapfile::try_map_file(detail::mapped_file_params_base p)
{
    bool priv = false;
    bool readonly = false;
    void* data =
            ::BOOST_IOSTREAMS_FD_MMAP(
                    const_cast<char*>(p.hint),
                    size_,
                    readonly ? PROT_READ : (PROT_READ | PROT_WRITE),
                    priv ? MAP_PRIVATE : MAP_SHARED,
                    handle_,
                    p.offset );
    if (data == MAP_FAILED) {
        cleanup_and_throw("failed mapping file");
        return;
    }
    data_ = static_cast<char*>(data);
}

void klogmapfile::map_file(detail::mapped_file_params_base& p)
{
    try {
            try_map_file(p);
    } catch (const std::exception&) {
        if (p.hint) {
            p.hint = 0;
            try_map_file(p);
        }
    }
}

void klogmapfile::open_file(detail::mapped_file_params_base p)
{
    bool readonly = false;
    // Open file
    int flags = (readonly ? O_RDONLY : O_RDWR);
    if (p.new_file_size != 0 && !readonly)
        flags |= (O_CREAT | O_TRUNC);
#ifdef _LARGEFILE64_SOURCE
        flags |= O_LARGEFILE;
#endif
    errno = 0;
    handle_ = ::open(p.path, flags, S_IRWXU);
    if (errno != 0) {
        cleanup_and_throw("failed opening file");
        return;
    }

    //--------------Set file size---------------------------------------------//

    if (p.new_file_size != 0 && !readonly)
        if (BOOST_IOSTREAMS_FD_TRUNCATE(handle_, p.new_file_size) == -1) {
            cleanup_and_throw("failed setting file size");
            return;
        }

    //--------------Determine file size---------------------------------------//

    bool success = true;
    if (p.length != static_cast<std::size_t>(-1)) {
        size_ = p.length;
    } else {
        struct stat info;
        success = BOOST_IOSTREAMS_FD_FSTAT(handle_, &info) != -1;
        size_ = info.st_size;
    }
    if (!success) {
        cleanup_and_throw("failed querying file size");
        return;
    }
}

bool klogmapfile::unmap_file() {
    return ::munmap(data_, size_) == 0;
}

void klogmapfile::close() {
    if (data_ == 0)
        return;
    bool error = false;
    error = !unmap_file() || error;
    if(handle_ >= 0) {
        error = ::close(handle_) != 0 || error;
    }
    clear(error);
}

void klogmapfile::cleanup_and_throw(const char* msg)
{
    int error = errno;
    if (handle_ >= 0)
        ::close(handle_);
    errno = error;
    clear(true);
}

void detail::mapped_file_params_base::normalize() {
    if (mode && flags) {
        throw_exception();
    }

    if (flags) {
        switch (flags) {
            case mapped_file_base::readonly:
            case mapped_file_base::readwrite:
            case mapped_file_base::priv:
                break;
            default:
                throw_exception();
        }
    }

    if (offset < 0) {
        throw_exception();
    }

    if (new_file_size < 0) {
        throw_exception();
    }
}

void detail::mapped_file_params_base::throw_exception() {
    char stack[4096] = {0};
    android_callstack(stack, sizeof(stack));
    xfatal2(TSF"%_", stack);
}
