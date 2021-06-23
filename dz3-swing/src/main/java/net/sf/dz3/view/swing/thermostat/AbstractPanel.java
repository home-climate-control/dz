package net.sf.dz3.view.swing.thermostat;

import net.sf.dz3.view.swing.ColorScheme;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

public abstract class AbstractPanel extends JPanel {

    protected abstract JPanel createControls();

    @SuppressWarnings("squid:S1199")
    protected final void createLayout(String name, JPanel chart) {

        setBackground(ColorScheme.offMap.background);

        var layout = new GridBagLayout();
        var cs = new GridBagConstraints();

        this.setLayout(layout);

        // VT: NOTE: squid:S1199 - SonarLint is not smart enough to realize that these
        // blocks are for readability

        {
            // Controls take the upper quarter of the display

            cs.fill = GridBagConstraints.HORIZONTAL;
            cs.gridx = 0;
            cs.gridy = 0;
            cs.gridwidth = GridBagConstraints.REMAINDER;
            cs.gridheight = 1;
            cs.weightx = 1;
            cs.weighty = 0;

            var controls = createControls();

            layout.setConstraints(controls, cs);
            this.add(controls);
        }

        {
            cs.gridy++;
            cs.gridheight = 1;
            cs.weighty = 1;
            cs.fill = GridBagConstraints.BOTH;

            layout.setConstraints(chart, cs);
            this.add(chart);

            chart.setPreferredSize(getPreferredSize());
            var bg = ColorScheme.offMap.background;
            var chartBg = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 0x00);
            chart.setBackground(chartBg);
        }

        // Really dirty, but really quick

        var template = BorderFactory.createTitledBorder(name);
        var border = BorderFactory.createTitledBorder(
                getBorder(),
                name,
                template.getTitleJustification(),
                template.getTitlePosition(),
                template.getTitleFont(),
                Color.WHITE);

        this.setBorder(border);
    }
}
