package net.sf.dz3r.view.swing;

/**
 * A pair of the content panel, and its corresponding cell.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class CellAndPanel<T, P> {
    public final EntityCell<T, P> cell;
    public final EntityPanel<T, P> panel;

    public CellAndPanel(EntityCell<T, P> cell, EntityPanel<T, P> panel) {
        this.cell = cell;
        this.panel = panel;
    }
}
