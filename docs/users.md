# How to work with users

## Get any account

You can get any account via `User.query(int id)`. Note that you are
not expected to do it: whenever you will need a User, there will be a way
to access it without needing to use the ID.

For example, if you'd like to get the author of a particular post, you
can do:

    User user = post.getAuthor();

This will give you access to the user's data:

    user.getBio(); // The user's bio
    user.getName(); // The user's name
    user.getAvatar() & user.getAvatarUrl(); // The user's profile picture
    user.isBanned(); // Is this user banned?

You can also get the user's ID via `user.getId()`, but as always this API
is tailored so you do not need the IDs.

As always you can get more details in the Javadoc.

## Your own account

You can get the account you logged-in with using `WildFyre.getMe();`.

This returns a LoggedUser, which you can use to perform some modifications
on your object.

For example, you can use:

    user.setBio("new bio here");

to change your biography, as well as `user.setName` and `user.setAvatar`.

You can also get a LoggedUser from any method that returns a User:

    User user = /*...*/;
    if(user.canEdit())
        LoggedUser me = user.asLogged();

This way you can set any data right on the spot.
