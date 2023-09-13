//
// Created by Administrator on 2020/8/24.
//

#ifndef ATHENA_ANDROID_KLOGMAPFILE_H
#define ATHENA_ANDROID_KLOGMAPFILE_H

#include <stdint.h>
#include <cstddef>

typedef int (openmode);
typedef int64_t stream_offset;

# if defined(_LARGEFILE64_SOURCE) && !defined(__APPLE__) && \
         (!defined(_FILE_OFFSET_BITS) || _FILE_OFFSET_BITS != 64) || \
     defined(_AIX) && !defined(_LARGE_FILES) || \
     defined(BOOST_IOSTREAMS_HAS_LARGE_FILE_EXTENSIONS)
/**/

    /* Systems with transitional extensions for large file support */

#  define BOOST_IOSTREAMS_FD_SEEK      lseek64
#  define BOOST_IOSTREAMS_FD_TRUNCATE  ftruncate64
#  define BOOST_IOSTREAMS_FD_MMAP      mmap64
#  define BOOST_IOSTREAMS_FD_STAT      stat64
#  define BOOST_IOSTREAMS_FD_FSTAT     fstat64
#  define BOOST_IOSTREAMS_FD_OFFSET    off64_t
# else
#  define BOOST_IOSTREAMS_FD_SEEK      lseek
#  define BOOST_IOSTREAMS_FD_TRUNCATE  ftruncate
#  define BOOST_IOSTREAMS_FD_MMAP      mmap
#  define BOOST_IOSTREAMS_FD_STAT      stat
#  define BOOST_IOSTREAMS_FD_FSTAT     fstat
#  define BOOST_IOSTREAMS_FD_OFFSET    off_t
# endif

class mapped_file_base {
public:
    enum mapmode {
        readonly = 1,
        readwrite = 2,
        priv = 4
    };
};

namespace detail {

    struct mapped_file_params_base {
        mapped_file_params_base()
                : flags(static_cast<mapped_file_base::mapmode>(0)),
                  mode(), offset(0), length(static_cast<std::size_t>(-1)),
                  new_file_size(0), hint(0)
        { }

        void normalize();
        void throw_exception();

    private:
        friend class mapped_file_impl;

    public:
        mapped_file_base::mapmode   flags;
        openmode         mode;  // Deprecated
        stream_offset               offset;
        std::size_t                 length;
        stream_offset               new_file_size;
        const char*                 hint;
        const char*                 path;
    };

} // End namespace detail.

class klogmapfile : public mapped_file_base {
public:
    klogmapfile();
    virtual ~klogmapfile();
    bool is_open();
    void open(detail::mapped_file_params_base &p);
    void close();
    void clear(bool error);
    char* data() const;
    bool unmap_file();
    void open_file(detail::mapped_file_params_base p);
    void map_file(detail::mapped_file_params_base& p);
    void try_map_file(detail::mapped_file_params_base p);
    void cleanup_and_throw(const char* msg);
    bool error() const { return error_; }

private:
    int handle_;
    char* data_;
    uint64_t  size_;
    bool  error_;
    detail::mapped_file_params_base params_;
};


#endif //ATHENA_ANDROID_KLOGMAPFILE_H
