package net.sf.dz3r.view.swing;

/**
 * A pair of the content panel, and its corresponding cell.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class CellAndPanel {
    public final EntityCell cell;
    public final EntityPanel panel;

    public CellAndPanel(EntityCell cell, EntityPanel panel) {
        this.cell = cell;
        this.panel = panel;
    }
}
