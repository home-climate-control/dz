package net.sf.dz3r.device.onewire;

import com.dalsemi.onewire.utils.OWPath;

public interface OWPathResolver {
    OWPath getPath(String address);
}
