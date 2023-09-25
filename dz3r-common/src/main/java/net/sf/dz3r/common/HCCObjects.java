package net.sf.dz3r.common;

public class HCCObjects {

    /**
     * Unlike {@link java.util.Objects#requireNonNull(Object, String)}, throws an {@link IllegalArgumentException}.
     *
     * @param target the object reference to check for nullity.
     * @param message detail message to be used in the event that a {@link IllegalArgumentException} is thrown.
     * @param <T> the type of the reference.
     * @return {@code obj} if not {@code null}.
     * @throws NullPointerException if {@code obj} is {@code null}.
     */
    public static <T> T requireNonNull(T target, String message) {
        if (target == null) {
            throw new IllegalArgumentException(message);
        }
        return target;
    }
}
