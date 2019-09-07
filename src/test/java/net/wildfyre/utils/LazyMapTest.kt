/*
 * Copyright 2019 Wildfyre.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.wildfyre.utils

import org.junit.Assert.*
import org.junit.Test
import java.util.*
import net.wildfyre.utils.LazyMap

class LazyMapTest {
    @Test()
    fun copyTest() {
        val map = mapOf(0 to "1", 1 to "2", 2 to "3")
        val lazyMap = LazyMap(map)
        assertEquals(lazyMap.size, 3) 
        assertEquals(lazyMap[0], "1")
        assertEquals(lazyMap[1], "2")
        assertEquals(lazyMap[2], "3")
    }
    @Test( expected = NullPointerException::class )
    fun copyNullTest() {
        val map: Map<Int, Int>? = null
        LazyMap(map)
    }
    @Test
    fun equalsTest() {
        val map1 = LazyMap<Int, Int>()
        val map2 = LazyMap<Int, Int>()
        assertEquals(map1, map2)
    }
    @Test
    fun isEmptyTest() {
        val map = LazyMap<Int, Int>()
        assertTrue(map.isEmpty())
    }
    @Test
    fun putAllTest() {
        val map = mapOf(0 to "1", 1 to "2", 2 to "3")
        val lazyMap1 = LazyMap<Int, String>()
        lazyMap1.putAll(map)
        assertEquals(lazyMap1.size, 3) 
        assertEquals(lazyMap1[0], "1")
        assertEquals(lazyMap1[1], "2")
        assertEquals(lazyMap1[2], "3")

        val lazyMap2 = LazyMap(HashMap<Int, String>())
        lazyMap2.putAll(map)
        assertEquals(lazyMap2.size, 3) 
        assertEquals(lazyMap2[0], "1")
        assertEquals(lazyMap2[1], "2")
        assertEquals(lazyMap2[2], "3")

        val lazyMap3 = LazyMap(HashMap<Int, Int>())
        lazyMap3.putAll(lazyMap3)
        assertEquals(lazyMap3.size, 0)
    }
}
