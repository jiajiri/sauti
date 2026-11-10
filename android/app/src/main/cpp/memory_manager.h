#pragma once

#include <cstddef>
#include <cstdint>
#include <sys/mman.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/stat.h>

namespace sauti::memory {

/**
 * Native Memory & Scratch Arena Controller.
 * Provides O(1) zero-allocation inference scratch buffers and safe zero-copy mmap.
 */
class MemoryArenaManager {
public:
    static constexpr size_t SCRATCH_ARENA_SIZE = 16 * 1024 * 1024; // 16 MB static pool

    static MemoryArenaManager& instance() {
        static MemoryArenaManager instance_;
        return instance_;
    }

    MemoryArenaManager(const MemoryArenaManager&) = delete;
    MemoryArenaManager& operator=(const MemoryArenaManager&) = delete;

    /**
     * Maps model weights read-only into process virtual memory space.
     * Pages remain backed by disk until accessed by the CPU.
     */
    void* mmapModel(const char* filepath, size_t* out_size) {
        if (!filepath || !out_size) return nullptr;

        int fd = open(filepath, O_RDONLY);
        if (fd < 0) return nullptr;

        struct stat sb;
        if (fstat(fd, &sb) < 0) {
            close(fd);
            return nullptr;
        }

        *out_size = static_cast<size_t>(sb.st_size);
        void* addr = mmap(nullptr, *out_size, PROT_READ, MAP_SHARED, fd, 0);
        close(fd);

        if (addr == MAP_FAILED) {
            return nullptr;
        }
        return addr;
    }

    /**
     * Unmaps memory pages.
     */
    void unmapModel(void* addr, size_t size) {
        if (addr && size > 0) {
            munmap(addr, size);
        }
    }

    /**
     * Returns pre-allocated scratch buffer pointer (Zero heap allocation at runtime).
     */
    uint8_t* getScratchArena() {
        return scratch_arena_;
    }

    /**
     * Evicts non-essential native caches upon system memory pressure.
     */
    void onMemoryTrim(bool isCritical) {
        if (isCritical) {
            // Advise kernel to release inactive memory pages immediately
            madvise(scratch_arena_, SCRATCH_ARENA_SIZE, MADV_DONTNEED);
        }
    }

private:
    MemoryArenaManager() {
        scratch_arena_ = new uint8_t[SCRATCH_ARENA_SIZE];
    }

    ~MemoryArenaManager() {
        delete[] scratch_arena_;
    }

    uint8_t* scratch_arena_;
};

} // namespace sauti::memory
