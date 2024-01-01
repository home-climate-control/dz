package com.homeclimatecontrol.hcc;

/**
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public final class Version {
    /**
     * Participates in mDNS announcements, and should be used by clients to determine compatibility.
     *
     * Follows <a href ="https://semver.org/#semantic-versioning-200">Semantic Versioning 2.0.0</a> conventions.
     */
    public static final String PROTOCOL_VERSION = "4.2.0";

    private Version() {

    }
}
