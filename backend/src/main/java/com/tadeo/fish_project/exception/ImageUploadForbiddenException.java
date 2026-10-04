package com.tadeo.fish_project.exception;

/*
Uploaded images aren't moderated, so in the public demo only admins may upload them
*/
public class ImageUploadForbiddenException extends RuntimeException {
    public ImageUploadForbiddenException() {
        super("Only admins may upload images");
    }
}
