#pragma once

#include <atomic>
#include <cstddef>
#include <cstdint>
#include <vector>
#include <algorithm>

namespace sauti::dsa {

/**
 * Single-Producer Single-Consumer (SPSC) Lock-Free Circular Ring Buffer.
 *
 * Characteristics:
 * - Thread Safety: Safe for concurrent access between 1 producer thread (e.g. AAudio recorder)
 *   and 1 consumer thread (e.g. Whisper inference loop) without mutexes or locks.
 * - Time Complexity: O(1) push and pop operations.
 * - Space Complexity: O(N) pre-allocated memory; strictly zero runtime allocations.
 * - Cache Friendly: Cache-line alignment on head and tail pointers to avoid false sharing.
 * - Power-of-two size enables fast bitwise masking instead of modulo division.
 */
template <typename T, size_t CapacityPowerOfTwo = 65536>
class SpscRingBuffer {
    static_assert((CapacityPowerOfTwo & (CapacityPowerOfTwo - 1)) == 0, "Capacity must be a power of two");

public:
    SpscRingBuffer() : buffer_(CapacityPowerOfTwo), head_(0), tail_(0) {}

    ~SpscRingBuffer() = default;

    // Disallow copy and assignment
    SpscRingBuffer(const SpscRingBuffer&) = delete;
    SpscRingBuffer& operator=(const SpscRingBuffer&) = delete;

    /**
     * Pushes a batch of elements into the ring buffer.
     * Called strictly by the Producer thread.
     *
     * @param data Pointer to source buffer
     * @param count Number of elements to write
     * @return Number of elements successfully written
     */
    size_t write(const T* data, size_t count) {
        if (!data || count == 0) return 0;

        const size_t current_head = head_.load(std::memory_order_relaxed);
        const size_t current_tail = tail_.load(std::memory_order_acquire);

        const size_t available = CapacityPowerOfTwo - (current_head - current_tail);
        const size_t to_write = std::min(count, available);

        if (to_write == 0) return 0;

        const size_t mask = CapacityPowerOfTwo - 1;
        const size_t idx = current_head & mask;

        const size_t first_chunk = std::min(to_write, CapacityPowerOfTwo - idx);
        std::copy(data, data + first_chunk, buffer_.data() + idx);

        if (to_write > first_chunk) {
            std::copy(data + first_chunk, data + to_write, buffer_.data());
        }

        head_.store(current_head + to_write, std::memory_order_release);
        return to_write;
    }

    /**
     * Reads a batch of elements from the ring buffer.
     * Called strictly by the Consumer thread.
     *
     * @param dest Pointer to destination buffer
     * @param count Maximum number of elements to read
     * @return Number of elements actually read
     */
    size_t read(T* dest, size_t count) {
        if (!dest || count == 0) return 0;

        const size_t current_tail = tail_.load(std::memory_order_relaxed);
        const size_t current_head = head_.load(std::memory_order_acquire);

        const size_t available = current_head - current_tail;
        const size_t to_read = std::min(count, available);

        if (to_read == 0) return 0;

        const size_t mask = CapacityPowerOfTwo - 1;
        const size_t idx = current_tail & mask;

        const size_t first_chunk = std::min(to_read, CapacityPowerOfTwo - idx);
        std::copy(buffer_.data() + idx, buffer_.data() + idx + first_chunk, dest);

        if (to_read > first_chunk) {
            std::copy(buffer_.data(), buffer_.data() + (to_read - first_chunk), dest + first_chunk);
        }

        tail_.store(current_tail + to_read, std::memory_order_release);
        return to_read;
    }

    /**
     * Inspects current number of available readable elements.
     */
    size_t size() const {
        const size_t current_head = head_.load(std::memory_order_acquire);
        const size_t current_tail = tail_.load(std::memory_order_acquire);
        return current_head - current_tail;
    }

    /**
     * Returns true if buffer is empty.
     */
    bool empty() const {
        return size() == 0;
    }

    /**
     * Resets pointers. Must only be called when both producer and consumer are idle.
     */
    void clear() {
        head_.store(0, std::memory_order_relaxed);
        tail_.store(0, std::memory_order_relaxed);
    }

    static constexpr size_t capacity() {
        return CapacityPowerOfTwo;
    }

private:
    std::vector<T> buffer_;

    // Align to 64-byte hardware cache line to eliminate false sharing between cores
    alignas(64) std::atomic<size_t> head_;
    alignas(64) std::atomic<size_t> tail_;
};

} // namespace sauti::dsa
