/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.datasketches.memory.internal;

import java.nio.ByteOrder;

import org.apache.datasketches.memory.MemoryRequestServer;
import org.apache.datasketches.memory.WritableBuffer;

/**
 * Implementation of {@link WritableBuffer} for heap-based, non-native byte order.
 *
 * @author Roman Leventov
 * @author Lee Rhodes
 */
final class HeapNonNativeWritableBufferImpl extends NonNativeWritableBufferImpl {
  private final Object unsafeObj;

  HeapNonNativeWritableBufferImpl(
      final Object unsafeObj,
      final long offsetBytes,
      final long capacityBytes,
      final int typeId,
      final long cumOffsetBytes,
      final MemoryRequestServer memReqSvr) {
    super(capacityBytes);
    this.unsafeObj = unsafeObj;
    this.offsetBytes = offsetBytes;
    this.capacityBytes = capacityBytes;
    this.typeId = removeNnBuf(typeId) | HEAP | BUFFER | NONNATIVE;
    this.cumOffsetBytes = cumOffsetBytes;
    this.memReqSvr = memReqSvr; //in ResourceImpl
    if ((this.owner != null) && (this.owner != Thread.currentThread())) {
      throw new IllegalStateException(THREAD_EXCEPTION_TEXT);
    }
    this.owner = Thread.currentThread();
  }

  @Override
  BaseWritableBufferImpl toWritableRegion(
      final long regionOffsetBytes,
      final long capacityBytes,
      final boolean readOnly,
      final ByteOrder byteOrder) {
    final long newOffsetBytes = offsetBytes + regionOffsetBytes;
    final long newCumOffsetBytes = cumOffsetBytes + regionOffsetBytes;
    int typeIdOut = removeNnBuf(typeId) | BUFFER | REGION | (readOnly ? READONLY : 0);

    if (Util.isNativeByteOrder(byteOrder)) {
      typeIdOut |= NATIVE;
      return new HeapWritableBufferImpl(
          unsafeObj, newOffsetBytes, capacityBytes, typeIdOut, newCumOffsetBytes, memReqSvr);
    } else {
      typeIdOut |= NONNATIVE;
      return new HeapNonNativeWritableBufferImpl(
          unsafeObj, newOffsetBytes, capacityBytes, typeIdOut, newCumOffsetBytes, memReqSvr);
    }
  }

  @Override
  BaseWritableMemoryImpl toWritableMemory(final boolean readOnly, final ByteOrder byteOrder) {
    int typeIdOut = removeNnBuf(typeId) | MEMORY | (readOnly ? READONLY : 0);

    if (byteOrder == ByteOrder.nativeOrder()) {
      typeIdOut |= NATIVE;
      return new HeapWritableMemoryImpl(
          unsafeObj, offsetBytes, capacityBytes, typeIdOut, cumOffsetBytes, memReqSvr);
    } else {
      typeIdOut |= NONNATIVE;
      return new HeapNonNativeWritableMemoryImpl(
          unsafeObj, offsetBytes, capacityBytes, typeIdOut, cumOffsetBytes, memReqSvr);
    }
  }

  @Override
  BaseWritableBufferImpl toDuplicate(final boolean readOnly, final ByteOrder byteOrder) {
    int typeIdOut = removeNnBuf(typeId) | BUFFER | DUPLICATE | (readOnly ? READONLY : 0);

    if (byteOrder == ByteOrder.nativeOrder()) {
      typeIdOut |= NATIVE;
      return new HeapWritableBufferImpl(
          unsafeObj, offsetBytes, capacityBytes, typeIdOut, cumOffsetBytes, memReqSvr);
    } else {
      typeIdOut |= NONNATIVE;
      return new HeapNonNativeWritableBufferImpl(
          unsafeObj, offsetBytes, capacityBytes, typeIdOut, cumOffsetBytes, memReqSvr);
    }
  }

  @Override
  Object getUnsafeObject() {
    return unsafeObj;
  }

}
