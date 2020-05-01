package net.sf.dz3.device.sensor.impl.tcp;

public class TcpConnectionSignature implements Comparable<TcpConnectionSignature> {

    public final int port;
    public final boolean secure;
    public final String password;

    public TcpConnectionSignature(int port, boolean secure, String password) {

        this.port = port;
        this.secure = secure;
        this.password = password;
    }

    @Override
    public final boolean equals(Object other) {

        if (other == null) {
            return false;
        }

        return toString().equals(other.toString());
    }

    @Override
    public final int hashCode() {
        return toString().hashCode();
    }

    @Override
    public final String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append("(");

        render(sb);

        sb.append(")");

        return sb.toString();
    }

    protected void render(final StringBuilder sb) {

        sb.append("port ").append(port);
        sb.append(secure ? ",secure" : "");
        sb.append(secure ? "," : "").append(secure ? password : "");
    }

    @Override
    public int compareTo(TcpConnectionSignature o) {

        return toString().compareTo(o.toString());
    }
}
