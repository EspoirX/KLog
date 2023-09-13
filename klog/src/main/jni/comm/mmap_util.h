// Tencent is pleased to support the open source community by making Mars available.
// Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.

// Licensed under the MIT License (the "License"); you may not use this file except in 
// compliance with the License. You may obtain a copy of the License at
// http://opensource.org/licenses/MIT

// Unless required by applicable law or agreed to in writing, software distributed under the License is
// distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
// either express or implied. See the License for the specific language governing permissions and
// limitations under the License.

/*
 * mmap_util.h
 *
 *  Created on: 2016-2-22
 *      Author: yanguoyue
 */

#ifndef KLOG_MMAP_UTIL_H_
#define KLOG_MMAP_UTIL_H_

#include "klogmapfile.h"

bool IsMmapFileOpenSucc(klogmapfile& _mmmap_file);

bool OpenMmapFile(const char* _filepath, unsigned int _size, klogmapfile& _mmmap_file);

void CloseMmapFile(klogmapfile& _mmmap_file);

#endif /* MMAP_UTIL_H_ */
