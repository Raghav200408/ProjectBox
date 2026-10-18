package com.boxproject.BoxProject.exception;

public class VenueMemberNotFoundException extends RuntimeException {
    public VenueMemberNotFoundException(Long memberId) {
        super("Venue member not found: " + memberId);
    }
}
