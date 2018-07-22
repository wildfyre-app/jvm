# WFLib-Java User manual

## Introduction

This file will mirror the API documentation
[from the website](https://api.wildfyre.net/docs/index.html).

This document is not a complete documentation but a user manual: that
means that we will present to you the basic methods but we won't
go into many details. If you'd like a complete overview of the available
methods or are searching for more details on any method, check out the
Javadoc.

## Before using the API

Your first step when using the API will be to connect to the server.

You can either use the user's name & password, or the token.

    WildFyre.connect("username here", "password here");

The token is of the form `9d36a784f7bc641b9d0f7a000a96b6563b987956`.
After you connected, you can retrieve the current token with:

    String token = WildFyre.getToken();

Now, you can save the token. The next time you need to connect
to the server, you can just use the token again:

    WildFyre.connect("token here");

It is recommended to NEVER save the user's password: if you have
to save something, save the token.
