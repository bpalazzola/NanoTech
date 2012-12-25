package me.main__.nanotech.irc;

public class IRCLoadingException extends Exception {
    public IRCLoadingException() {
    }

    public IRCLoadingException(final String message) {
        super(message);
    }

    public IRCLoadingException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public IRCLoadingException(final Throwable cause) {
        super(cause);
    }
}
