import java.awt.*;
import javax.swing.JButton;
import javax.swing.ImageIcon;

public class Card extends JButton {
	private int index;
	private String imagePath;
	public final String BACK_TEXT = "hello";
	public static final int NUM_OF_FRONT_IMAGES = 19;
	private ImageIcon backIcon;
	private ImageIcon frontIcon;
	private boolean hasImages = false;
	
	/**
	 *	Construct a card
	 *	@param i index of the card
	 *	@param hasImages If all the images are available then images will be used
	 */
	public Card(int i, String imagePath, boolean hasImages){
		this.imagePath = imagePath;
		this.hasImages = hasImages;
		if(hasImages)
			backIcon = new ImageIcon(imagePath + "/back.jpg");
		else 
			setText(BACK_TEXT);
		index = i;
	}
	
	
	@Override
	public void paintComponent(Graphics g){
		super.paintComponent(g);
		
		if(hasImages){
			setIcon(getScaledImage(backIcon));
			if(frontIcon != null)
				setDisabledIcon(getScaledImage(frontIcon));
		}
		
	}
	
	/**
	 *	@return the index of the card
	 */
	public int getIndex(){
		return index;
	}
	
	/**
	 *	show card and disable it
	 *	@param value	the value of the card
	 */
	public void revealCard(int value){
		if(hasImages){
			if(frontIcon == null)
				frontIcon = new ImageIcon(imagePath + "/" + value + ".jpg");
			else
				setDisabledIcon(getScaledImage(frontIcon));
		} else
			setText(String.valueOf(value));
		
		setEnabled(false);
	}
	
	/**
	 *	hide card and enable it
	 */
	public void hideCard(){
		if(hasImages)
			setIcon(getScaledImage(backIcon));
		else
			setText(BACK_TEXT);
		
		setEnabled(true);
	}
	
	/**
	 * returns a scaled version of the image
	 * @param icon	the image to scale
	 * @return
	 */
	private ImageIcon getScaledImage(ImageIcon icon){
		Image img = icon.getImage();
		return new ImageIcon(img.getScaledInstance(getWidth(), getHeight(), Image.SCALE_FAST));
	}
}