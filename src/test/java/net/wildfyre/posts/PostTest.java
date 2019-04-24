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

package net.wildfyre.posts;

import net.wildfyre.areas.Area;
import net.wildfyre.areas.Areas;
import net.wildfyre.http.RequestTest;
import net.wildfyre.users.Users;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class PostTest {

    @Before
    public void before() {
        RequestTest.Companion.connectToTestDB();
    }

    @Test
    public void myPosts(){
        List<Post> posts = Users.me()
            .asLogged()
            .postsList();

        assertEquals(3, posts.size());

        Post p1 = posts.stream().filter(it -> !it.isAnonymous()).findFirst().orElseThrow(RuntimeException::new);
        assertFalse(p1.toString(), p1.isAnonymous());
        assertTrue(p1.toString(), p1.hasSubscribed());
        assertTrue(p1.toString(), p1.isActive());
        assertTrue(p1.toString(), p1.author().isPresent());
        assertEquals(p1.toString(), Users.me(), p1.author().get());

        Post p2 = posts.stream().filter(PostData::isAnonymous).findFirst().orElseThrow(RuntimeException::new);
        assertTrue(p2.toString(), p2.isAnonymous());
        assertTrue(p2.toString(), p2.hasSubscribed());
        assertTrue(p2.toString(), p2.isActive());
        assertFalse(p2.toString(), p2.author().isPresent());
    }

    @Test
    public void createDraft(){
        Area a = Areas.INSTANCE.get("sample").orElseThrow(RuntimeException::new);

        Draft d = a.draft()
            .setText("This is a test")
            .setIsAnonymous(false)
            .subscribe()
            .save();

        assertNotEquals(-1, d.postID);

        d.delete();
    }

}
