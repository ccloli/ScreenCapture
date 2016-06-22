
import java.util.TimerTask;
import java.util.Timer;
import javax.swing.*;
import java.io.*;
import java.util.*;
import java.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.imageio.ImageIO;

// Frame has some bugs, so uses JPanel
// http://stackoverflow.com/questions/16565310/java-bufferedimage-returns-black-image-from-canvas
public class ScreenCapture implements ActionListener {
	private BufferedImage tmpImage;
	private BufferedImage finalImage;
	private Robot robot;
	private Rectangle rectangle;
	private Button captureAllBtn;
	private Button captureAreaBtn;
	private Button saveCaptureBtn;
	private CaptureCanvas drawCanvas;
	private JFrame frame;
	private JPanel topPanel;
	private JScrollPane drawPanel;
	private JPanel leftBtnPanel;
	private JPanel rightBtnPanel;
	private Button colorPickBtn;

	public static void main(String[] args) {
		ScreenCapture capture = new ScreenCapture();
	}

	public ScreenCapture() {
		try {
			robot = new Robot();
		}
		catch (AWTException error) {
			System.out.println("AWTException Error: " + error);
		}

		// Layout part
		frame = new JFrame("Screen Capture");
		topPanel = new JPanel();
		//Panel topPanel = new Panel();
		topPanel.setLayout(new BorderLayout());

		leftBtnPanel = new JPanel();
		captureAllBtn = new Button("全屏截图");
		captureAreaBtn = new Button("区域截图");
		leftBtnPanel.add(captureAllBtn);
		leftBtnPanel.add(captureAreaBtn);

		rightBtnPanel = new JPanel();
		saveCaptureBtn = new Button("保存截图");
		rightBtnPanel.add(saveCaptureBtn);

		topPanel.add("West", leftBtnPanel);
		// topPanel.add("East", rightBtnPanel);
		captureAllBtn.addActionListener(this);
		captureAreaBtn.addActionListener(this);
		saveCaptureBtn.addActionListener(this);

		// drawCanvas = new CaptureCanvas(new BufferedImage(1, 1, 1));
		drawPanel = new JScrollPane();

		frame.setLayout(new BorderLayout());
		frame.add("North", topPanel);
		//frame.add("Center", drawPanel);
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public void captureImage() {
		rectangle = new Rectangle(0, 0, 0, 0);

		for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
			rectangle = rectangle.union(gd.getDefaultConfiguration().getBounds());
		}
		// hide window immediately
		Point originalPosition = frame.getLocation();
		frame.setLocation(rectangle.width, rectangle.height);
		frame.dispose();

		tmpImage = robot.createScreenCapture(rectangle);
		frame.setVisible(true);
		frame.setLocation(originalPosition.x, originalPosition.y);
	}

	public void clipImage() {
		ImageClipedEventResponder eventHandler = new ImageClipedEventResponder() {
			@Override
			public void implementClipArea(Rectangle r) {
				tmpImage = tmpImage.getSubimage(r.x, r.y, r.width, r.height);
				editImage();
			}
		};
		SelectImageArea area = new SelectImageArea(tmpImage, eventHandler);
	}

	public void editImage(){
		// drawPanel.remove(drawCanvas);
		// drawCanvas = new CaptureCanvas(tmpImage);
		// frame.setSize(tmpImage.getWidth(), tmpImage.getHeight());
		// drawPanel.add(drawCanvas);
		// drawPanel.setSize(tmpImage.getWidth(), tmpImage.getHeight());
		// drawCanvas.getGraphics().drawImage(tmpImage, 0, 0, null);
		int preW = frame.getWidth(), preH = frame.getHeight();
		
		if (drawPanel != null) frame.remove(drawPanel);
		if (colorPickBtn != null) rightBtnPanel.remove(colorPickBtn);
		topPanel.add("East", rightBtnPanel);

		drawCanvas = new CaptureCanvas(tmpImage);
		drawPanel = new JScrollPane(drawCanvas, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		drawCanvas.setAutoscrolls(true);
		colorPickBtn = drawCanvas.createColorPickBtn();
		rightBtnPanel.add("West", colorPickBtn);
		frame.add("Center", drawPanel);

		frame.pack();
		Rectangle screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		int baseW = frame.getWidth() - drawPanel.getViewport().getWidth(), 
		    baseH = frame.getHeight() - drawPanel.getViewport().getHeight();
		frame.setSize(
			Math.max(Math.max(Math.min(tmpImage.getWidth() / 2 + baseW, screenSize.width), preW), 300), 
			Math.max(Math.max(Math.min(tmpImage.getHeight() / 2 + baseH, screenSize.height), preH), 200)
		);
		frame.setVisible(true);
	}

	public void saveImage() {
		// System.out.println(new SimpleDateFormat("yyyymmddhhmmss").format(new Date()));

		File dir = new File("./ScreenCapture/");
		if (!dir.exists()) dir.mkdirs();
		
		File file = new File("./ScreenCapture/" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".png");
		try {
			ImageIO.write(finalImage, "png", file);

			JLabel message = new JLabel("文件已保存在 " + file.getCanonicalPath());
			message.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent event) {
					try {
						Desktop.getDesktop().open(file);
					}
					catch (IOException error) {
						System.out.println("IOException Error: " + error);
					}
				}
			});
			frame.add("South", message);
			// frame.pack();
			// frame.setVisible(true);
			Timer timer = new Timer();
			TimerTask removeMessage = new TimerTask(){
				@Override
				public void run() {
					frame.remove(message);
					frame.pack();
					frame.setVisible(true);
				}
			};
			timer.schedule(removeMessage, 3000);
		}
		catch (IOException error) {
			System.out.println("IOException Error: " + error);
		}
	}

	public void actionPerformed(ActionEvent event) {
		if (event.getSource() == captureAllBtn) {
			captureImage();
			editImage();
		}
		else if (event.getSource() == captureAreaBtn) {
			captureImage();
			clipImage();
		}
		else if (event.getSource() == saveCaptureBtn) {
	  		finalImage = drawCanvas.tmpImage;
	  		frame.remove(drawPanel);
	  		topPanel.remove(rightBtnPanel);
			saveImage();
			frame.pack();
		}
	}
}


interface ImageClipedEventListener {
	void implementClipArea(Rectangle r);
}

// ImageClipedEvent
class ImageClipedEvent {
	private ImageClipedEventListener eventListener;

	public void addListener(ImageClipedEventListener el) {
		eventListener = el;
	}

	public void implement(Rectangle r) {
		eventListener.implementClipArea(r);
	}
}

// ImageClipedEvent
class ImageClipedEventResponder implements ImageClipedEventListener {
	@Override
	public void implementClipArea(Rectangle r) {}
}

// Canvas is too old and it's hard to use, so we use JPanel directly.
class CaptureCanvas extends JPanel implements MouseListener, MouseMotionListener {
	private Point p1, p2;
	public BufferedImage tmpImage;
	private Graphics2D g2;
	private Color color = Color.BLACK;

	public CaptureCanvas(BufferedImage _tmpimage) {
		addMouseListener(this);
		addMouseMotionListener(this);
		setSize(_tmpimage.getWidth(), _tmpimage.getHeight());
		setPreferredSize(new Dimension(_tmpimage.getWidth(), _tmpimage.getHeight()));
		tmpImage = _tmpimage;
	}

	public Button createColorPickBtn() {
		Button pickButton = new Button("选择颜色");
		pickButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				color = JColorChooser.showDialog(null, "选择颜色", color);
			}
		});
		return pickButton;
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.drawImage(tmpImage, 0, 0, null);
		//g2.drawLine(p1.x, p1.y, p2.x, p2.y);
		// g.drawLine(p1.x,p1.y,p2.x,p2.y);
		// p1 = p2;
	}

	public void mousePressed (MouseEvent event) {
		p1 = event.getPoint();
	}

	public void mouseDragged (MouseEvent event) {
		p2 = event.getPoint();
		//Graphics page = getGraphics();
		// draw in tmpImage.getGraphics() instead of JPanel.getGraphics()
		g2 = (Graphics2D)tmpImage.getGraphics();
		g2.setColor(color);
		g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2.drawLine(p1.x, p1.y, p2.x, p2.y);
		g2.dispose();
		p1 = p2;
		repaint();
	}
	
	public void mouseReleased(MouseEvent event) {}
	public void mouseClicked(MouseEvent event) {}
	public void mouseEntered(MouseEvent event) {}
	public void mouseExited(MouseEvent event) {}
	public void mouseMoved(MouseEvent event) {}
}

class SelectImageArea extends JPanel implements MouseListener, MouseMotionListener {
	private JFrame frame = new JFrame();
	private BufferedImage tmpImage;
	private JLabel imageLabel;
	private Point p1 = new Point(0, 100), p2 = new Point(0, 100);
	private int x = 0, y = 0, w = 0, h = 0;
	private ImageClipedEventResponder eventListener;

	public SelectImageArea(BufferedImage _tmpimage, ImageClipedEventResponder el) {
		tmpImage = _tmpimage;
		eventListener = el;
		//frame.setLayout(new BorderLayout());
		//frame.add("Center", panel);
		frame.add(this);

		setSize(tmpImage.getWidth(), tmpImage.getHeight());
		setPreferredSize(new Dimension(tmpImage.getWidth(), tmpImage.getHeight()));
		frame.setSize(tmpImage.getWidth(), tmpImage.getHeight());
		frame.setPreferredSize(new Dimension(tmpImage.getWidth(), tmpImage.getHeight()));

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gd = ge.getScreenDevices();
		int minX = 0, minY = 0;
		for (int i = 0; i < gd.length; i++) {
			Rectangle bounds = gd[i].getDefaultConfiguration().getBounds();
			if (bounds.x < minX) {
				minX = bounds.x;
			}
			if (bounds.y < minY) {
				minY = bounds.y;
			}
		}

		addMouseListener(this);
		addMouseMotionListener(this);

		frame.setUndecorated(true);
		frame.setLocation(minX, minY);
		frame.pack();
		frame.setVisible(true);
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.drawImage(tmpImage, 0, 0, null);
		g.setColor(new Color(0f, 0f, 0f, 0.75f));
		//g.fillRect(0, 0, tmpImage.getWidth(), tmpImage.getHeight());
		
		// g.fillRect(0, 0, tmpImage.getWidth(), Math.min(p1.y, p2.y));
		// g.fillRect(Math.max(p2.x, p1.x), Math.min(p1.y, p2.y), tmpImage.getWidth() - Math.max(p2.x, p1.x), Math.abs(p2.y - p1.y));
		// g.fillRect(0, Math.max(p2.y, p1.y), tmpImage.getWidth(), tmpImage.getHeight() - Math.max(p2.y, p1.y));
		// g.fillRect(0, Math.min(p1.y, p2.y), Math.min(p1.x, p2.x), Math.abs(p2.y - p1.y));
		
		x = Math.min(p1.x, p2.x);
		y = Math.min(p1.y, p2.y);
		w = Math.abs(p2.x - p1.x);
		h = Math.abs(p2.y - p1.y);
		g.fillRect(0, 0, tmpImage.getWidth(), y);
		g.fillRect(x + w, y, tmpImage.getWidth() - x - w, h);
		g.fillRect(0, y + h, tmpImage.getWidth(), tmpImage.getHeight() - y - h);
		g.fillRect(0, y, x, h);
		g.setColor(new Color(1f, 1f, 1f, 0.75f));
		g.drawRect(x, y, w, h);

		if (w != 0 || h != 0) {
			Font font = new Font("Arial", Font.BOLD, 16);
			g.setFont(font);
			FontMetrics metrics = g.getFontMetrics(font);
			String size = "X: " + x + " / Y: " + y + " / W: " + w + " / H: " + h;

			g.setColor(new Color(0f, 0f, 0f, 0.25f));
			int textBaseX = Math.max(x + w - metrics.stringWidth(size) - 5, 10), textBaseY = Math.min(y + h + metrics.getHeight() / 2 + metrics.getAscent(), tmpImage.getHeight() - metrics.getHeight() + metrics.getAscent() - 5);
			g.drawString(size, textBaseX + 1, textBaseY + 1);
			g.drawString(size, textBaseX - 1, textBaseY - 1);
			g.drawString(size, textBaseX + 1, textBaseY - 1);
			g.drawString(size, textBaseX - 1, textBaseY + 1);
			g.drawString(size, textBaseX + 1, textBaseY);
			g.drawString(size, textBaseX, textBaseY + 1);
			g.drawString(size, textBaseX - 1, textBaseY);
			g.drawString(size, textBaseX, textBaseY - 1);
			
			g.setColor(Color.WHITE);
			g.drawString(size, textBaseX, textBaseY);
		}
	}

	public void mousePressed (MouseEvent event) {
		if (SwingUtilities.isRightMouseButton(event)) {
			if (w != 0 && h != 0) {
				p1 = p2;
			}
			else {
				frame.setVisible(false);
				frame.dispose();
			}
			repaint();
		}
		else if (SwingUtilities.isLeftMouseButton(event)) {
			p1 = event.getPoint();
		}
	}

	public void mouseDragged (MouseEvent event) {
		if (SwingUtilities.isLeftMouseButton(event)) {
			p2 = event.getPoint();
			repaint();
		}
	}

	public void mouseClicked(MouseEvent event) {
		if (event.getClickCount() == 2 && w > 0 && h > 0) {
			eventListener.implementClipArea(new Rectangle(x, y, w, h));
			frame.setVisible(false);
			frame.dispose();
		}
	}

	public void mouseReleased(MouseEvent event) {}
	public void mouseEntered(MouseEvent event) {}
	public void mouseExited(MouseEvent event) {}
	public void mouseMoved(MouseEvent event) {}
}