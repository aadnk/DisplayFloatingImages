package com.comphenix.example.nametags;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;

public class GifImageMessage {
	private static final int DEFAULT_TIMEOUT = 8000;
	
	/**
	 * Represents every frame in the GIF.
	 */
	protected ImmutableList<ImageFrame> frames;
	
	/**
	 * Construct a new image message representing an animated GIF image.
	 * @param gif - the URI pointing to the file or network resource with the GIF image.
	 * @param height - the number of lines in each image message.
	 * @param imgChar - the character used to represent a pixel in the image.
	 */
	public GifImageMessage(final URI gif, int height, char imgChar) throws IOException {
		if ("file".equalsIgnoreCase(gif.getSchemeSpecificPart())) {
			frames = readGif(Files.newInputStreamSupplier(new File(gif)));
		} else {
			frames = readGif(new InputSupplier<InputStream>() {
				@Override
				public InputStream getInput() throws IOException {
					URLConnection connection = gif.toURL().openConnection();
					connection.setRequestProperty("Content-Type", "image/gif");
					connection.setUseCaches(false);
					connection.setDoOutput(true);
					connection.setConnectTimeout(DEFAULT_TIMEOUT);
					return connection.getInputStream();
				}
			});
		}
		initializeFrames(height, imgChar);
	}
	
	/**
	 * Construct a new image message representing an animated GIF image.
	 * @param stream - the stream of data with the GIF image.
	 * @param height - the number of lines in each image message.
	 * @param imgChar - the character used to represent a pixel in the image.
	 */
	public GifImageMessage(InputSupplier<? extends InputStream> stream, int height, char imgChar) throws IOException {
		this.frames = readGif(stream);
		initializeFrames(height, imgChar);
	}

	/**
	 * Retrieve every frame in the GIF image message.
	 * @return Every frame.
	 */
	public ImmutableList<ImageFrame> getFrames() {
		return frames;
	}
	
	/**
	 * Initialize every loaded frame.
	 * @param height - the desired frame height.
	 * @param imgChar - the pixel character.
	 */
	protected void initializeFrames(int height, char imgChar) {
		Map<Integer, NameTagMessage> recycle = Maps.newHashMap();
		double maximumHeight = findMaximumHeight(frames);
		
		// Use that to scale each image down
		for (ImageFrame frame : frames) {
			int imageHeight = (int) ((frame.image.getHeight() / maximumHeight) * height);
			NameTagMessage previous = recycle.get(imageHeight);
			
			// Construct name tag messages
			if (previous != null) {
				frame.message = new NameTagMessage(previous, frame.image, imgChar);
			} else {
				frame.message = new NameTagMessage(frame.image, imageHeight, imgChar);
				recycle.put(imageHeight, frame.getMessage());
			}
		}
	}
	
	protected int findMaximumHeight(ImmutableList<ImageFrame> frames) {
		int maxHeight = 0;
		
		// Determine the largest image size
		for (ImageFrame frame : frames) {
			maxHeight = Math.max(maxHeight, frame.image.getHeight());
		} 
		return maxHeight; 
	}
	
	private ImmutableList<ImageFrame> readGif(InputSupplier<? extends InputStream> stream) throws IOException {
		InputStream input =	null;
		ImageFrame[] frames = null;
		boolean threw = true;
		
		try {
		    ImageReader reader = (ImageReader) ImageIO.getImageReadersByFormatName("gif").next();
		    
		    input = stream.getInput();
		    reader.setInput(ImageIO.createImageInputStream(input));
		    
		    frames = readGIF(reader);
			threw = false;
		} finally {
			Closeables.close(input, threw);
		}
		return ImmutableList.copyOf(frames);
	} 
	
	// Source: https://stackoverflow.com/questions/8933893/convert-animated-gif-frames-to-separate-bufferedimages-java
	private ImageFrame[] readGIF(ImageReader reader) throws IOException {
	    ArrayList<ImageFrame> frames = new ArrayList<ImageFrame>(2);

	    int width = -1;
	    int height = -1;

	    IIOMetadata metadata = reader.getStreamMetadata();
	    if (metadata != null) {
	        IIOMetadataNode globalRoot = (IIOMetadataNode) metadata.getAsTree(metadata.getNativeMetadataFormatName());

	        NodeList globalScreenDescriptor = globalRoot.getElementsByTagName("LogicalScreenDescriptor");

	        if (globalScreenDescriptor != null && globalScreenDescriptor.getLength() > 0) {
	            IIOMetadataNode screenDescriptor = (IIOMetadataNode) globalScreenDescriptor.item(0);

	            if (screenDescriptor != null) {
	                width = Integer.parseInt(screenDescriptor.getAttribute("logicalScreenWidth"));
	                height = Integer.parseInt(screenDescriptor.getAttribute("logicalScreenHeight"));
	            }
	        }
	    }

	    BufferedImage master = null;
	    Graphics2D masterGraphics = null;

	    for (int frameIndex = 0;; frameIndex++) {
	        BufferedImage image;
	        try {
	            image = reader.read(frameIndex);
	        } catch (IndexOutOfBoundsException io) {
	            break;
	        }

	        if (width == -1 || height == -1) {
	            width = image.getWidth();
	            height = image.getHeight();
	        }

	        IIOMetadataNode root = (IIOMetadataNode) reader.getImageMetadata(frameIndex).getAsTree("javax_imageio_gif_image_1.0");
	        IIOMetadataNode gce = (IIOMetadataNode) root.getElementsByTagName("GraphicControlExtension").item(0);
	        int delay = Integer.valueOf(gce.getAttribute("delayTime"));
	        String disposal = gce.getAttribute("disposalMethod");

	        int x = 0;
	        int y = 0;

	        if (master == null) {
	            master = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	            masterGraphics = master.createGraphics();
	            masterGraphics.setBackground(new Color(0, 0, 0, 0));
	        } else {
	            NodeList children = root.getChildNodes();
	            for (int nodeIndex = 0; nodeIndex < children.getLength(); nodeIndex++) {
	                Node nodeItem = children.item(nodeIndex);
	                if (nodeItem.getNodeName().equals("ImageDescriptor")) {
	                    NamedNodeMap map = nodeItem.getAttributes();
	                    x = Integer.valueOf(map.getNamedItem("imageLeftPosition").getNodeValue());
	                    y = Integer.valueOf(map.getNamedItem("imageTopPosition").getNodeValue());
	                }
	            }
	        }
	        masterGraphics.drawImage(image, x, y, null);

	        BufferedImage copy = new BufferedImage(master.getColorModel(), master.copyData(null), master.isAlphaPremultiplied(), null);
	        frames.add(new ImageFrame(copy, delay, disposal));

	        if (disposal.equals("restoreToPrevious")) {
	            BufferedImage from = null;
	            for (int i = frameIndex - 1; i >= 0; i--) {
	                if (!frames.get(i).disposal.equals("restoreToPrevious") || frameIndex == 0) {
	                    from = frames.get(i).image;
	                    break;
	                }
	            }

	            master = new BufferedImage(from.getColorModel(), from.copyData(null), from.isAlphaPremultiplied(), null);
	            masterGraphics = master.createGraphics();
	            masterGraphics.setBackground(new Color(0, 0, 0, 0));
	        } else if (disposal.equals("restoreToBackgroundColor")) {
	            masterGraphics.clearRect(x, y, image.getWidth(), image.getHeight());
	        }
	    }
	    reader.dispose();

	    return frames.toArray(new ImageFrame[frames.size()]);
	}

	/**
	 * Represents a single image frame.
	 * @author Kristian
	 */
	public static class ImageFrame {
	    private final int delay;
	    private final BufferedImage image;
	    private final String disposal;
	    
	    // Associated image message
	    private NameTagMessage message;

	    /**
	     * Construct a new image frame. 
	     * @param image - the buffered image.
	     * @param delay - the delay (in 10s of milliseconds)
	     * @param disposal - the disposal method (handled by the loader).
	     */
	    private ImageFrame(BufferedImage image, int delay, String disposal) {
	        this.image = image;
	        this.delay = delay;
	        this.disposal = disposal;
	    }
	    
	    public int getDelay() {
	        return delay;
	    }
	    
	    public NameTagMessage getMessage() {
			return message;
		}
	}
}
