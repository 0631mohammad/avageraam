/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ir.avageram.com.exoplayer2.upstream;

/**
 * An allocation within a byte array.
 * <p>
 * The allocation's length is obtained by calling {@link Allocator#getIndividualAllocationLength()}
 * on the {@link Allocator} from which it was obtained.
 */
public final class Allocation {

  /**
   * The array containing the allocated space. The allocated space might not be at the start of the
   * array, and so {@link #translateOffset(int)} method must be used when indexing into it.
   */
  public final byte[] data;

  private final int offset;

  /**
   * @param data The array containing the allocated space.
   * @param offset The offset of the allocated space within the array.
   */
  public Allocation(byte[] data, int offset) {
    this.data = data;
    this.offset = offset;
  }

  /**
   * Translates a zero-based offset into the allocation to the corresponding {@link #data} offset.
   *
   * @param offset The zero-based offset to translate.
   * @return The corresponding offset in {@link #data}.
   */
  public int translateOffset(int offset) {
    return this.offset + offset;
  }

}
