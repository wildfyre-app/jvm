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
import net.wildfyre.posts.Post;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import com.eclipsesource.json.JsonObject;

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

        assertTrue(p1.equals(p1));
        assertFalse(p1.equals(p2));

        assertEquals(new Post((PostData)p1), p1);

        assertEquals(p2.commentsList().size(), 2);
        assertEquals(p2.commentsList().get(0), p2.comments().findFirst().get());

        Comment c1 = p2.commentsList().get(0);

        JsonObject data = new JsonObject();
        data.set("id", c1.ID());
        JsonObject author = new JsonObject();
        author.set("user", c1.author().getID());
        data.set("author", author);
        data.set("created", c1.created().toString());
        data.set("text", c1.text());
        data.set("imageURL", c1.imageURL().orElse(""));
        Comment c2 = new Comment(p2, data);
        assertEquals(c1, c2);

        assertEquals(c1.hashCode(), c2.ID());

        assertEquals(c1.area(), p2.area());
        assertEquals(c1.post(), p2);
    }

    @Test
    public void createDraft(){
        Area a = Areas.INSTANCE.get("sample").orElseThrow(RuntimeException::new);

        Draft d = a.draft()
            .setText("This is a test (1)")
            .setIsAnonymous(false)
            .subscribe()
            .save();

        assertNotEquals(-1, d.postID);

//        Post p = d.publish();
//        assertEquals(p.text(), "This is a test (1)");
//        p.delete();

        d = a.draft()
            .setText("This is a test (2)")
            .setIsAnonymous(false)
            .subscribe();

//        p = d.publish();
//        assertEquals(p.text(), "This is a test (2)");
//        p.delete();

        d = a.draft()
            .setText("This is a test (3)")
            .setIsAnonymous(true)
            .subscribe()
            .save()
            .setText("This is a test (4)")
            .save();

        assertEquals(d.text(), "This is a test (4)");

        d.delete();
    }

}
