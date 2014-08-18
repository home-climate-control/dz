package net.sf.dz3.view.swing.thermostat;

/**
 * Intended to hold a data sample for the {@link Chart variable color chart}.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2012
 */
public class TintedValue {

	public final double value;
	public final double tint;
	
	public TintedValue(double value, double tint) {
		
		this.value = value;
		this.tint = tint;
	}
	
	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("TintedValue(").append(value).append(", ").append(tint);
		sb.append(")");
		
		return sb.toString();
	}
}
