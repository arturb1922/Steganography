import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.WritableRaster;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {

    /**
     * Reads in the input parameters and starts the process by creating a Main object.
     * @param args  input parameters
     */
    public static void main(String[] args) {
        String message = "Ala ma kota";
        String filepath = "C:\\Users\\Artur\\IdeaProjects\\Steganography\\output.png";
        BufferedImage image = loadImage(filepath);

        System.out.println("Do you want to encrypt or decrypt data?");
        System.out.println("1. Encrypt\n2. Decrypt");
        Scanner s = new Scanner(System.in);
        String temp = s.nextLine();

        switch (temp){
            case "1":
            {
                addMessageToImage(message,image);
                break;
            }

            case "2":
            {
                retrieveMessageFromImage(image,message);
                break;
            }

            default:break;

        }

    }

    /**
     * Loads an image from the given input path.
     * It is important that the BufferedImage received from the ImageIO.read() function
     * has a ColorModel which does not support the saving of alpha channels. Therefore
     * we have to create a new BufferedImage with a different ColorModel that supports
     * changing the alpha channel.
     * @param input     path to image
     * @return          BufferedImage of image
     */
    public static BufferedImage loadImage(String input) {
        try {
            File file = new File(input);
            BufferedImage in = ImageIO.read(file);
            BufferedImage newImage = new BufferedImage(
                    in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_RGB);

            Graphics2D g = newImage.createGraphics();
            g.drawImage(in, 0, 0, null);
            g.dispose();

            return newImage;
        } catch (IOException e) {
            System.err.println("Could not read image with give file path.");
            e.printStackTrace();
        }

        return null;
    }


    /**
     * Retrieves a message from a given input image by collecting the last 2 bits
     * of each Red, Green, Blue, and Alpha component of each pixel starting at the
     * top left of the picture. The retrieval process stops once the end block, which is
     * "11111111" is encountered. The message is then saved to a text file.
     * @param image     The input image as a BufferedImage
     */
    public static  void retrieveMessageFromImage(BufferedImage image,String secretMessage) {
        String binaryMessage = convertStringToBinary(secretMessage);
        String retrievedMessage = "";
        String temp="";
        outerloop:
        for (int row = 0; row < image.getHeight(); row++) {
            for (int column = 0; column < image.getWidth(); column++) {
                int rgb = image.getRGB(column, row);
                Color color = new Color(rgb, false);

                ArrayList<String> rgba = getColorBinaries(color);

                String block = retrieveLastBinaryPair(rgba);
                temp=temp+block;

                if(temp.length()>=8)
                {
                    String pom="";
                    pom=temp.substring(0,8);
                    int charCode=Integer.parseInt(pom,2);
                    retrievedMessage+=Character.toString((char)charCode);


                    if(temp.length()==8) {

                        temp = "";
                    }
                    if(temp.length()==9) {

                        temp = temp.substring(temp.length() - 1);
                    }
                    if(temp.length()==10)
                    {
                        temp=temp.substring(temp.length()-2);
                    }
                    if(temp.length()==11)
                    {
                        temp=temp.substring(temp.length()-3);
                    }

                    if(retrievedMessage.equals(secretMessage))
                        break outerloop;
                    }


            }
        }

        System.out.println(retrievedMessage);
    }

    /**
     * Retrieves the last 2 bits of the Red, Green, Blue, and Alpha channel of a
     * given ArrayList containing the values for these channels as a binary String.
     * @param rgba      The binary strings of the Red, Green, Glue, and Alpha channels of a pixel
     * @return          A concatenation of all last 2 bits of the binary strings.
     */
    private static String retrieveLastBinaryPair(ArrayList<String> rgba) {
        String out = "";

        for (String e : rgba)
            out += e.substring(7);

        return out;
    }

    /**
     * The main method to add a message to an image. It runs through every pixel starting
     * at the top left and adds a character represented by 8 bits to the 4 color channels
     * (Red, Green, Blue, Alpha) of the pixel. It replaces the last 2 bits of the binary value
     * for each channel by 2 bits of the character binaries. The adding process is finished
     * by adding an end block of "11111111" to a pixel.
     * @param secretMessage     The message to add to the image.
     * @param image             The image as BufferedImage to which the message is added.
     */
    private static void addMessageToImage(String secretMessage, BufferedImage image) {
        String binaryMessage = convertStringToBinary(secretMessage);

        outerloop:
        for (int row = 0; row < image.getHeight(); row++) {
            for (int column = 0; column < image.getWidth(); column++) {


                int start = row * image.getWidth() + column*3;
                int end = start + 3;


                String messageBlock;
                if(start>=binaryMessage.length())
                {
                    break outerloop;
                }
                if (end <= binaryMessage.length()) {
                     messageBlock=binaryMessage.substring(start,end);
                } else {
                    messageBlock = binaryMessage.substring(start);
                }

                int rgb = image.getRGB(column, row);
                Color oldColor = new Color(rgb, false);

                ArrayList<String> rgbaOld = getColorBinaries(oldColor);
                ArrayList<String> rgbaNew = new ArrayList<>();

                if(messageBlock.length()==3) {
                    for (int i = 0; i < rgbaOld.size(); i++)
                        rgbaNew.add(rgbaOld.get(i).substring(0, 7) + messageBlock.substring(i, i + 1));
                }
                else {
                    int counter=0;
                    for (int i = 0; i < messageBlock.length(); i++) {
                        rgbaNew.add(rgbaOld.get(i).substring(0, 7) + messageBlock.substring(i, i + 1));
                        counter++;
                    }
                    for(int i=counter;i<rgbaOld.size();i++)
                    {
                        rgbaNew.add(rgbaOld.get(i));
                    }

                }
                ArrayList<Integer> newColorComponents = rgbaToInt(rgbaNew);
                newColorComponents = checkMaxValuesOfComponents(newColorComponents);

                Color newColor = new Color(newColorComponents.get(0), newColorComponents.get(1), newColorComponents.get(2));
                image.setRGB(column, row, newColor.getRGB());

            }
        }

        saveImage(image);
    }

    /**
     * Checks whether the changed values of the color components are larger than 255.
     * If they are they are replaced with 255. This functions as a safeguard in case something
     * goes wrong somewhere else as the maximum value of an 8 bit Integer representation can
     * only be 255 max. However, I left this in for safety reasons.
     * @param input     The changed color component values as Integers.
     * @return          Color component values which are not larger than 255.
     */
    public static ArrayList<Integer> checkMaxValuesOfComponents(ArrayList<Integer> input) {
        for (Integer e : input)
            input.set(input.indexOf(e), e < 255 ? e : 255);

        return input;
    }

    /**
     * Converts an ArrayList of Binaries as Strings to an ArrayList of their Integer values.
     * @param input     An ArrayList of color component values as binaries as strings.
     * @return          An ArrayList of color component values as Integers.
     */
    public static ArrayList<Integer> rgbaToInt(ArrayList<String> input) {
        ArrayList<Integer> out = new ArrayList<>();

        for (String e : input)
            out.add(Integer.parseInt(e, 2));

        return out;
    }

    /**
     * Converts the Integer values of the color components to Binary values as Strings.
     * @param color     The color which color components are wanted.
     * @return          An ArrayList of Strings containing the color component values as binaries.
     */
    public static ArrayList<String> getColorBinaries(Color color) {
        ArrayList<String> out = new ArrayList<>();
        out.add(intToBinaryString(color.getRed()));
        out.add(intToBinaryString(color.getGreen()));
        out.add(intToBinaryString(color.getBlue()));
        //out.add(intToBinaryString(color.getAlpha()));

        return out;
    }

    /**
     * Saves a BufferedImage to a .png file at the location of this program.
     * @param image     The BufferedImage to be saved.
     */
    public static void saveImage(BufferedImage image) {
        try {
            File output = new File("output.png");
            ImageIO.write(image, "png", output);
            System.out.println("Saved output to: " + output.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Could not write the output to .png file.");
            e.printStackTrace();
        }
    }

    /**
     * Saves a String to a .txt file at the location of this program.
     * @param text      The String to be saved.
     */
    public static void saveText(String text) {
        try {
            File output = new File("retrieved-text.txt");
            FileOutputStream fos = new FileOutputStream(output);

            byte[] contentInBytes = text.getBytes();

            fos.write(contentInBytes);
            fos.flush();
            fos.close();

            System.out.println("Saved output to: " + output.getAbsolutePath());

        } catch (FileNotFoundException e) {
            System.err.println("Could not create output text. Check program writing permissions.");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Could not write output to file.");
            e.printStackTrace();
        }
    }

    /**
     * Converts a String of Characters to a String of their values as Binaries.
     * @param input     The String to be converted.
     * @return          A String containing the binary values of the input characters.
     */
    public static String convertStringToBinary(String input) {
        String out = "";

        char[] charMessage = input.toCharArray();
        for (char c : charMessage){
            String temp = Integer.toBinaryString(c);
            out += fillString(temp);
        }

        return out;
    }

    /**
     * Converts an Integer to a Binary String.
     * @param input     The Integer to be converted.
     * @return          A String containing the binary value of the input Integer.
     */
    public static String intToBinaryString(int input) {
        return fillString(Integer.toBinaryString(input));
    }

    /**
     * Adds 0's to the beginning of a String of binaries to ensure that the string
     * is exactly 8 bits long. This is necessary as the Integer.toBinaryString() does not always
     * return a String with 8 bits. The leading bits are omitted from the Integer function if
     * they are 0's.
     * @param input     A String of bits.
     * @return          A String of exactly 8 bits.
     */
    public static String fillString(String input) {
        for (int i = input.length(); i < 8; i++)
            input = "0" + input;

        return input;
    }
}