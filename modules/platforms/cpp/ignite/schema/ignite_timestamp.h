/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#pragma once

#include <cstdint>

namespace ignite {

/**
 * @brief A moment of time.
 *
 * This is modeled after java.time.Instant.
 */
class ignite_timestamp {
public:
    /**
     * Default constructor.
     */
    constexpr ignite_timestamp() noexcept = default;

    /**
     * Constructor.
     *
     * @param seconds Number of seconds since 00:00 hours, Jan 1, 1970 UTC.
     * @param nanos Fractional second component in nanoseconds, from 0 to 999,999,999.
     */
    constexpr ignite_timestamp(std::int64_t seconds, std::int32_t nanos)
        : seconds(seconds)
        , nanos(nanos) {
        // TODO: check that arguments are in valid ranges.
    }

    /**
     * Gets the number of seconds since 00:00 hours, Jan 1, 1970 UTC.
     */
    constexpr std::int64_t get_epoch_second() const noexcept { return seconds; }

    /**
     * Gets the number of nanoseconds from the start of the second.
     */
    constexpr std::int32_t get_nano() const noexcept { return nanos; }

    /**
     * Compare to another value.
     *
     * @param other Instance to compare to.
     * @return Zero if equals, negative number if less, and positive if greater.
     */
    constexpr int compare(const ignite_timestamp &other) const noexcept {
        if (seconds != other.seconds) {
            return seconds < other.seconds ? -1 : 1;
        }
        return nanos - other.nanos;
    }

private:
    /** Number of seconds since 00:00 hours, Jan 1, 1970 UTC. */
    std::int64_t seconds = 0;

    /** Fractional second component in nanoseconds. */
    std::int32_t nanos = 0;
};

/**
 * @brief Comparison operator.
 *
 * @param lhs First value.
 * @param rhs Second value.
 * @return true If the first value is equal to the second.
 */
constexpr bool operator==(const ignite_timestamp &lhs, const ignite_timestamp &rhs) noexcept {
    return lhs.compare(rhs) == 0;
}

/**
 * @brief Comparison operator.
 *
 * @param lhs First value.
 * @param rhs Second value.
 * @return true If the first value is not equal to the second.
 */
constexpr bool operator!=(const ignite_timestamp &lhs, const ignite_timestamp &rhs) noexcept {
    return lhs.compare(rhs) != 0;
}

/**
 * @brief Comparison operator.
 *
 * @param lhs First value.
 * @param rhs Second value.
 * @return true If the first value is less than the second.
 */
constexpr bool operator<(const ignite_timestamp &lhs, const ignite_timestamp &rhs) noexcept {
    return lhs.compare(rhs) < 0;
}

/**
 * @brief Comparison operator.
 *
 * @param lhs First value.
 * @param rhs Second value.
 * @return true If the first value is less than or equal to the second.
 */
constexpr bool operator<=(const ignite_timestamp &lhs, const ignite_timestamp &rhs) noexcept {
    return lhs.compare(rhs) <= 0;
}

/**
 * @brief Comparison operator.
 *
 * @param lhs First value.
 * @param rhs Second value.
 * @return true If the first value is greater than the second.
 */
constexpr bool operator>(const ignite_timestamp &lhs, const ignite_timestamp &rhs) noexcept {
    return lhs.compare(rhs) > 0;
}

/**
 * @brief Comparison operator.
 *
 * @param lhs First value.
 * @param rhs Second value.
 * @return true If the first value is greater than or equal to the second.
 */
constexpr bool operator>=(const ignite_timestamp &lhs, const ignite_timestamp &rhs) noexcept {
    return lhs.compare(rhs) >= 0;
}

} // namespace ignite
