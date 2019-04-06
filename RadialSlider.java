// RadialSlider is free software: you can redistribute it and/or modify
//     it under the terms of the GNU General Public License as published by
//     the Free Software Foundation, either version 3 of the License, or
//     (at your option) any later version.
//
//     RadialSlider is distributed in the hope that it will be useful,
//     but WITHOUT ANY WARRANTY; without even the implied warranty of
//     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//     GNU General Public License for more details.
//
//     You should have received a copy of the GNU General Public License
//     along with RadialSlider.  If not, see <https://www.gnu.org/licenses/>.

import java.awt.Adjustable;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * JComponent for selecting values in a radial way
 * 
 * @author Miguel Alejandro Moreno Barrientos, (C) 2019
 * @version 0.1.0
 */
public class RadialSlider extends JPanel implements Adjustable, FocusListener, MouseListener, 
												MouseMotionListener, MouseWheelListener, KeyListener 
{
	protected static final long serialVersionUID = 1L;

	/** Color on disabling */
	protected static final Color DISABLED_COLOR = new Color( 200, 200, 200 );
	
	/** don't show ticks */
	public static final int NO_TICKS = 0;
	/** show block ticks */
	public static final int TICKS_BLOCK = 0b01;
	/** show unit ticks */
	public static final int TICKS_UNIT = 0b10;

	/** default double format */
	public static final DecimalFormat DEF_DOUBLE = new DecimalFormat( "#.###" );
	/** default degree format */
	public static final DecimalFormat DEF_DEGREE = new DecimalFormat( "#.##º" );
	/** default radian format */
	public static final DecimalFormat DEF_RADIAN = new DecimalFormat( "#.####rad" );
	
	/** current value */
	protected double value;
	/** minimum value (included) */
	protected final double minValue;
	/** maximum value (excluded) */
	protected final double maxValue;
	/** small increment in degrees */
	protected int unitIncrement;
	/** big increment in degrees */
	protected int blockIncrement;
	/** show ticks */
	protected int ticks = TICKS_BLOCK;
	/** max line width */
	protected float lineWidth = 1.5f;
	/** current DecimalFormat */
	protected DecimalFormat df = DEF_DOUBLE;

	// flags
	protected boolean isShowingAxis = false;
	protected boolean isTextVisible = true;
	protected boolean isFocused = false;
	
	// listener lists
	protected List<ChangeListener> changeListenerList = new ArrayList<>();
	protected List<AdjustmentListener> adjustListenerList = new ArrayList<>();
	
	// private
	private static final double PI2 = 2*Math.PI;
	private boolean automaticFont = true;	
	private int keySpeed;	
	private final Timer keyTimer = new Timer( 250, e -> {
		RadialSlider.this.keyTimer.setDelay( 40 );
		setAngle( getAngle() + Math.toRadians( keySpeed ) );
		fireStateChanged();
		fireStateAdjustingOnKey(AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED, true);
	});	
	
	
	//////////////////
	// Constructors
	//////////////////
	
	/**
	 * Constructor for angle selection with value=0, minValue=0, maxValue=360, 
	 * unitIncrement=1 <small>(degrees)</small>, blockIncrement=45 <small>(degrees)</small>
	 */
	public RadialSlider() 
	{
		this( 0., 0., 360. );
		df = DEF_DEGREE;
	}
	
	/**
	 * Constructor with initial value, minValue and maxValue.<br/>
	 * unitIncrement=1 <small>(degrees)</small>, blockIncrement=45 <small>(degrees)</small>
	 * 
	 * @param value initial value
	 * @param minValue minimum value
	 * @param maxValue maximum value
	 */
	public RadialSlider( double value, double minValue, double maxValue )
	{
		this.minValue = minValue;
		this.maxValue = maxValue;
		setValue( value );

		// initial config
		setForeground( Color.GRAY );
		setFocusable(true);
		unitIncrement = 1;
		blockIncrement = 45;
		
		// register listeners
		addMouseListener( this );
		addMouseMotionListener( this );
		addMouseWheelListener( this );
		addKeyListener( this );
		addFocusListener( this );
	}
	
	
	/////////////////////
	// Getters/Setters
	/////////////////////
	
	/**
	 * Sets the double value of the RadialSlider and repaint
	 * 
	 * @param value value as double in {@code [minValue,maxValue)}
	 * @return this component
	 */
	public RadialSlider setValue( double value )
	{
		// adjust value
		this.value = Math.max( Math.min( value, maxValue ), minValue );
		
		// notify change to listeners
		fireStateChanged();
		fireStateAdjusting( 
						AdjustmentEvent.ADJUSTMENT_LAST, AdjustmentEvent.TRACK, getValue(), false );
		
		// update UI
		repaint();
		
		return this;
	}

	/**
	 * Gets the double value of the RadialSlider
	 * 
	 * @return current value as double
	 */
	public double getDoubleValue() { return value; }
	
	/**
	 * Gets the minimum value of the RadialSlider
	 * 
	 * @return minimum value (included)
	 */
	public double getDoubleMinimum() { return minValue; }
	
	/**
	 * Gets the maximum value of the RadialSlider
	 * 
	 * @return maximum value (excluded)
	 */
	public double getDoubleMaximum() { return maxValue; }
	
	/**
	 * Gets RadialSlider angle
	 * 
	 * @return the angle in radians [0,2&#x03C0;)
	 */
	public double getAngle()
	{
		return map( value, minValue, maxValue, 0, PI2 );
	}
	
	/**
	 * Sets angle (and value from angle) in radians and repaint
	 * 
	 * @param angle angle in radians
	 * @return this component
	 */
	public RadialSlider setAngle( double angle ) 
	{ 
		value = map( (angle % PI2 + PI2) % PI2, 0, PI2, minValue, maxValue );
		repaint();
		return this;
	}
	
	/**
	 * @return current value format based on RadialSlider double value
	 */
	public DecimalFormat getDecimalFormat() { return df; }
	
	/**
	 * Sets value format on output based on RadialSlider double value
	 * 
	 * @param df angle format. RadialSlider include defaults {@link #DEF_DOUBLE}, 
	 *           {@link #DEF_DEGREE}, {@link #DEF_RADIAN}
	 * @return this component
	 */
	public RadialSlider setDecimalFormat( DecimalFormat df )
	{
		if ( !this.df.equals(df) )
		{
			this.df = df;
			repaint();
		}
		return this;
	}

	/**
	 * @return current max line width
	 */
	public float getLineWidth() { return lineWidth; }
	
	/**
	 * Sets max line width
	 * 
	 * @param lineWidth max line width
	 * @return this component
	 */
	public RadialSlider setLineWidth( float lineWidth ) 
	{
		this.lineWidth = lineWidth;
		repaint();
		return this;
	}
	
	/**
	 * @return <i>true</i> if value text is visible 
	 */
	public boolean isTextVisible() { return isTextVisible; }
	
	/**
	 * Show/Hide value text
	 * 
	 * @param isTextVisible <i>true</i> for showing text
	 * @return this component
	 */
	public RadialSlider setTextVisible( boolean isTextVisible ) 
	{
		if ( this.isTextVisible != isTextVisible )
		{
			this.isTextVisible = isTextVisible;
			repaint();
		}
		return this;
	}
	
	/**
	 * @return <i>true</i> if axis are visibles
	 */
	public boolean isShowingAxis() { return isShowingAxis; }
	
	/**
	 * Show/Hide axis 
	 * 
	 * @param isShowingAxis <i>true</i> for showing axis
	 * @return this component
	 */
	public RadialSlider setShowingAxis( boolean isShowingAxis ) 
	{
		if ( this.isShowingAxis != isShowingAxis )
		{
			this.isShowingAxis = isShowingAxis;
			repaint();
		}
		return this;
	}
	
	/**
	 * @return {@link #NO_TICKS}, {@link #TICKS_UNIT},
	 *         {@link #TICKS_BLOCK}, 
	 *         {@link #TICKS_UNIT} | {@link #TICKS_BLOCK}
	 */
	public int getTicks() { return ticks; }
	
	/**
	 * Show/Hide small and/or big ticks (based on <i>unitIncrement</i> and <i>blockIncrement</i>)
	 * 
	 * @param ticks {@link #NO_TICKS}, {@link #TICKS_UNIT},
	 *              {@link #TICKS_BLOCK}, 
	 *              {@link #TICKS_UNIT} | {@link #TICKS_BLOCK}
	 * @return this component
	 */
	public RadialSlider setTicks( int ticks ) 
	{
		if ( this.ticks != ticks )
		{
			this.ticks = ticks;
			repaint();
		}
		return this;
	}	
	
	@Override
	public Font getFont() 
	{
		if ( automaticFont )
			return new Font( Font.MONOSPACED, Font.BOLD, 
							 getWidth() / Math.max( getText().length(), 7 ) ); 
		return super.getFont();
	}

	/**
	 * Set angle text font or automatic font
	 * 
	 * @param font <i>null</i> for automatic font
	 */
	@Override
	public void setFont( Font font ) 
	{
		automaticFont = font == null;
		if ( !automaticFont )
			super.setFont( font );
		else if ( isTextVisible )
			repaint();
	}
	
	/**
	 * Gets the text shown in the RadialSlider
	 * 
	 * @return the shown text
	 */
	public String getText() { return df.format( value ); }

	
	///////////////////////////
	// FocusListener methods
	///////////////////////////
	
	@Override
	public void focusLost( FocusEvent e ) 
	{
		isFocused = false;
		repaint();
	}
	
	@Override
	public void focusGained( FocusEvent e ) 
	{
		isFocused = true;
		repaint();
	}

	
	////////////////////////////
	// ChangeListener methods
	////////////////////////////
	
	/**
	 * Adds a ChangeListener
	 * 
	 * @param listener ChangeListener to add
	 * @return this component
	 */
	public RadialSlider addChangeListener( ChangeListener listener )
	{
		changeListenerList.add( listener );
		return this;
	}
	
	/**
	 * Returns an array of all the ChangeListeners added with <b>{@link #addChangeListener}</b>
	 * 
	 * @return all of the ChangeListeners added or an emptyarray if no listeners have been added
	 */
	public ChangeListener[] getChangeListeners()
	{
		return changeListenerList.stream().toArray( ChangeListener[]::new );
	}

	/**
	 * Removes a ChangeListener
	 * 
	 * @param listener ChangeListener to remove
	 * @return <i>true</i> if the listener is registered
	 */
	public boolean removeChangeListener( ChangeListener listener )
	{
		return changeListenerList.remove( listener );
	}


	////////////////////////
	// Adjustable methods
	////////////////////////
	
	@Override
	public void addAdjustmentListener( AdjustmentListener listener ) 
	{
		adjustListenerList.add( listener );
	}

	/**
	 * @return maximum value as floor <b>integer</b> from double max value
	 */
	@Override
	public int getMaximum() { return (int) maxValue; }

	/**
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void setMaximum( int max ) throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException();		
	}

	/**
	 * @return minimum value as floor <b>integer</b> from double min value
	 */
	@Override
	public int getMinimum() { return (int) minValue; }

	/**
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void setMinimum( int min ) throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException();		
	}
	
	/**
	 * @return {@link java.awt.Adjustable#NO_ORIENTATION}
	 */
	@Override
	public int getOrientation() { return NO_ORIENTATION; }

	/**
	 * Small angle increment/decrement in degrees
	 * 
	 * @return unit increment in degrees, default <b>1</b>
	 */
	@Override
	public int getUnitIncrement() { return unitIncrement; }

	/**
	 * Sets small angle increment/decrement in degrees
	 */
	@Override
	public void setUnitIncrement( int inc ) { unitIncrement = inc; }

	/**
	 * Big angle increment/decrement in degrees
	 * 
	 * @return block increment in degrees, default <b>45</b>
	 */
	@Override
	public int getBlockIncrement() { return blockIncrement; }

	/**
	 * Sets big angle increment/decrement in degrees
	 */
	@Override
	public void setBlockIncrement( int inc ) { blockIncrement = inc; }

	/**
	 * Gets the integer value of the RadialSlider
	 * 
	 * @return current value as integer
	 */
	@Override
	public int getValue() { return (int) value; }

	/**
	 * Sets the double value of the RadialSlider
	 * 
	 * @param value value as integer
	 */
	@Override
	public void setValue( int value )
	{
		setValue( (double) value );
	}

	@Override
	public int getVisibleAmount() { return 0; }

	/**
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void setVisibleAmount(int v) throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeAdjustmentListener( AdjustmentListener listener ) 
	{
		adjustListenerList.remove( listener );
	}

	
	///////////////////////////
	// Mouse control methods
	///////////////////////////
	
	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) 
	{
		if ( !isEnabled() )  return;
		
		requestFocusInWindow();
		
		// update angle
		adjustAngleToMouseLocation(e);
		
		// notify event to listeners
		fireStateAdjusting( AdjustmentEvent.ADJUSTMENT_FIRST, AdjustmentEvent.TRACK, getValue(), 
							true );
	}

	@Override
	public void mouseReleased(MouseEvent e) 
	{
		if ( !isEnabled() )  return;
		
		//notify event to listeners
		fireStateAdjusting( AdjustmentEvent.ADJUSTMENT_LAST, AdjustmentEvent.TRACK, getValue(), 
							false );
		fireStateChanged();
	}

	@Override
	public void mouseDragged(MouseEvent e) 
	{
		if ( !isEnabled() )  return;
		
		// update angle
		adjustAngleToMouseLocation(e);

		// notify event to listeners
		fireStateAdjusting( AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED, AdjustmentEvent.TRACK, 
							getValue(), true );
	}

	@Override
	public void mouseMoved(MouseEvent e) {}

	@Override
	public void mouseWheelMoved( MouseWheelEvent e ) 
	{
		if ( !isEnabled() )  return;
		
		// update angle
		setAngle( getAngle() - Math.toRadians( e.getPreciseWheelRotation() * getUnitIncrement() ) ); 
		
		// notify event to listeners
		fireStateAdjusting( AdjustmentEvent.ADJUSTMENT_LAST, 
							e.getPreciseWheelRotation() > 0 
							? AdjustmentEvent.UNIT_DECREMENT 
							: AdjustmentEvent.UNIT_INCREMENT, 
							getValue(), false );
		fireStateChanged();
	}


	/////////////////////////
	// KeyListener methods
	/////////////////////////
	
	@Override
	public void keyPressed( KeyEvent e )
	{
		if ( !isEnabled() )  return;

		switch ( e.getKeyCode() )
		{
			case KeyEvent.VK_LEFT: 
				keySpeed = getUnitIncrement();
				keyPressedHelper(e);
				break;
			case KeyEvent.VK_RIGHT: 
				keySpeed = -getUnitIncrement();
				keyPressedHelper(e);
				break;
			case KeyEvent.VK_UP: 
				keySpeed = getBlockIncrement();
				keyPressedHelper(e);
				break;
			case KeyEvent.VK_DOWN: 
				keySpeed = -getBlockIncrement();
				keyPressedHelper(e);
				break;
		}
	}
	
	@Override
	public void keyReleased( KeyEvent e ) 
	{
		// stop keyTimer
		keySpeed = 0;
		keyTimer.stop();
		
		// notify key event
		fireStateAdjustingOnKey( AdjustmentEvent.ADJUSTMENT_LAST, false );
		fireStateChanged();
	}
	
	@Override
	public void keyTyped( KeyEvent e ) {}
	
	
	//////////////////
	// Draw methods
	//////////////////
	
	@Override
	protected void paintComponent(Graphics g) 
	{
		super.paintComponent(g);
		
		final Graphics2D g2 = (Graphics2D) g;
		
		// config graphics
		g2.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING,
	  				 		 RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, 
			  			 	 RenderingHints.VALUE_ANTIALIAS_ON );
		g2.setRenderingHint( RenderingHints.KEY_RENDERING, 
			  			 	 RenderingHints.VALUE_RENDER_QUALITY );
		g2.setRenderingHint( RenderingHints.KEY_ALPHA_INTERPOLATION, 
			  			 	 RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY );
		g2.setRenderingHint( RenderingHints.KEY_COLOR_RENDERING, 
			  			 	 RenderingHints.VALUE_COLOR_RENDER_QUALITY );
		g2.setRenderingHint( RenderingHints.KEY_DITHERING, 
			  			 	 RenderingHints.VALUE_DITHER_ENABLE );

		g2.setColor( isEnabled() ? getForeground() : DISABLED_COLOR );

		final int w2 = getWidth()/2, h2 = getHeight()/2;
		
		// -- DRAW --
		
		// axis
		if ( isShowingAxis() )
		{
			g2.setStroke( new BasicStroke( Math.min( 0.5f, lineWidth ), 
								BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
								1f, new float[] { 2f, 0f, 2f }, 0f ) );
			g2.drawLine( w2, 0, w2, getHeight() );
			g2.drawLine( 0, h2, getWidth(), h2 );
		}

		g2.setStroke( new BasicStroke( 
				lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ) );

		AffineTransform ori = g2.getTransform();
		
		// unit ticks
		if ( (ticks&TICKS_UNIT) == TICKS_UNIT )
		{
			g2.translate( w2, h2 );
			g2.scale ( 1, -(getHeight()-lineWidth) / (getWidth()-lineWidth) );
			
			for ( int i = 0; i <= 360 / getUnitIncrement(); i++ )
			{
				g2.drawLine( (int) ( ( w2 - lineWidth ) * 0.95 ), 0, (int) ( w2 - lineWidth ), 0 );
				g2.rotate( Math.toRadians( getUnitIncrement() ) );
			}
			
			g2.setTransform( ori );
		}

		// block ticks
		if ( (ticks&TICKS_BLOCK) == TICKS_BLOCK )
		{
			g2.translate( w2, h2 );
			g2.scale ( 1, -(getHeight()-lineWidth) / (getWidth()-lineWidth) );
			
			for ( int i = 0; i <= 360 / getBlockIncrement(); i++ )
			{
				g2.drawLine( (int) ( ( w2 - lineWidth ) * 0.9 ), 0, (int) ( w2 - lineWidth ), 0 );
				g2.rotate( Math.toRadians( getBlockIncrement() ) );
			}
			
			g2.setTransform( ori );
		}
		
		// circumference		
		g2.drawArc( (int) Math.floor(lineWidth/2), 
					(int) Math.floor(lineWidth/2), 
					getWidth() - (int) Math.ceil(lineWidth), 
					getHeight() - (int) Math.ceil(lineWidth), 
					0, 360 );
			
		// arrow
		g2.translate( w2, h2 );
		g2.scale( 1, -(getHeight()-lineWidth) / (getWidth()-lineWidth) );
		g2.rotate( getAngle() );
		
		g2.drawLine( 0, 0, w2 - (int) lineWidth, 0 );
		
		// (arrowhead)
		g2.drawLine( w2 - (int) lineWidth, 0, 
					 w2 - (int) lineWidth - w2/5, w2/5 );
		g2.drawLine( w2 - (int) lineWidth, 0, 
					 w2 - (int) lineWidth - w2/5, -w2/5 );
		
		g2.setTransform( ori );
		
		// text
		if ( isTextVisible() )
		{
			g2.setFont( getFont() );
			final String txt = getText();
			final FontMetrics fm = g2.getFontMetrics( g2.getFont() );
			final int x = w2 - fm.stringWidth(txt) / 2,
					  y = h2 + ( fm.getHeight() + fm.getDescent() - fm.getAscent() ) / 2;
			g2.setColor( getBackground() );
			g2.drawString( txt, x+1, y+1 );
			g2.setColor( isEnabled() ? getForeground() : DISABLED_COLOR );
			g2.drawString( txt, x, y );
		}
		
		// focused bounds
		if ( isFocused )
		{
			g2.setStroke( new BasicStroke( 0.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
										   1f, new float[] { 2f, 0f, 2f }, 0f ) );
			g2.drawRect( 0, 0, getWidth() - 1, getHeight() - 1 );
		}
	}
	
	
	/////////////
	// Helpers
	/////////////
	
	/**
	 * Adjust angle to mouse position from component center
	 * 
	 * @param e the mouse event
	 */
	protected void adjustAngleToMouseLocation( MouseEvent e )
	{
		double angle = Math.atan2( ((double) getWidth() / getHeight()) * (e.getY() - getHeight()/2), 
								   getWidth()/2 - e.getX() ) + Math.PI;
		
		if ( e.isShiftDown() )  // adjust to block increment ticks
			angle = Math.round( angle / Math.toRadians( getBlockIncrement() ) ) 
				    * Math.toRadians( getBlockIncrement() );
		else if ( e.isControlDown() )  // adjust to unit increment ticks
			angle = Math.round( angle / Math.toRadians( getUnitIncrement() ) ) 
					* Math.toRadians( getUnitIncrement() );
		
		setAngle( angle ); 
	}
	
	/**
	 * Notify event to ChangeListeners
	 */
	protected void fireStateChanged()
	{
		if ( isEnabled() && !changeListenerList.isEmpty() )
			changeListenerList.stream().forEach( chl -> chl.stateChanged( new ChangeEvent(this) ) );
	}
	
	/**
	 * Notify event to AdjustListeners
	 * 
	 * @param id AdjustmentEvent id
	 * @param type AdjustmentEvent type
	 * @param value AdjustmentEvent value
	 * @param isAdjusting AdjustmentEvent isAdjusting
	 */
	protected void fireStateAdjusting( int id, int type, int value, boolean isAdjusting )
	{
		if ( isEnabled() && !adjustListenerList.isEmpty() )
		{
			final AdjustmentEvent evt = new AdjustmentEvent( this, id, type, value, isAdjusting );

			adjustListenerList.stream().forEach( al -> al.adjustmentValueChanged(evt) );
		}
	}
	
	/**
	 * Helper to create suitable AdjustmentEvent on increments
	 * 
	 * @param id AdjustmentEvent id
	 * @param isAdjusting AdjustmentEvent isAdjusting
	 */
	private void fireStateAdjustingOnKey( int id, boolean isAdjusting )
	{
		fireStateAdjusting( id, 
							keySpeed > 0
							? keySpeed == getUnitIncrement()
							  ? AdjustmentEvent.UNIT_INCREMENT
							  : AdjustmentEvent.BLOCK_INCREMENT
						    : keySpeed == -getUnitIncrement()
						      ? AdjustmentEvent.UNIT_DECREMENT
							  : AdjustmentEvent.BLOCK_DECREMENT,  
							getValue(), isAdjusting );
	}
	
	/**
	 * Update angle, notify adjusting event for key pressed and launch key-repeat timer 
	 * 
	 * @param e the key event
	 */
	private void keyPressedHelper( KeyEvent e )
	{
		// set angle
		double angle = getAngle() + Math.toRadians( keySpeed );
		
		if ( e.isShiftDown() )  // adjust to current increment ticks
			angle = Math.round( angle / Math.toRadians( keySpeed ) ) * Math.toRadians( keySpeed );
		
		setAngle( angle );
		
		// notify key event
		fireStateAdjustingOnKey( AdjustmentEvent.ADJUSTMENT_FIRST, true );
		
		// start key-repeat timer
		keyTimer.start();
	}
	
	/**
	 * Maps a double from [a,b] to [c,d]
	 * 
	 * @param x value to map
	 * @param a origin start
	 * @param b origin end
	 * @param c destiny start
	 * @param d destiny end
	 * @return mapped value
	 */
	private static double map( double x, double a, double b, double c, double d )
	{
		return c + ( d - c ) / ( b - a ) * ( x - a );
	}
}
