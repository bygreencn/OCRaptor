package mj.ocraptor.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.io.InputStream;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.Border;

import mj.ocraptor.configuration.Config;
import mj.ocraptor.javafx.GUIController;
import mj.ocraptor.javafx.Icon;

import org.apache.commons.lang3.StringUtils;

public class SplashScreen {
  private JLabel jLabelWithText;
  private JFrame frame;
  private Timer timer;

  private static Point mousePosition = null;
  private final static int LABEL_UPDATE_DELAY = 200;

  private final static int THREE_DOTS_COUNT = 3;
  private final static int STAGE_01 = 7;
  private final static int STAGE_02 = 11;
  private final static int STAGE_03 = 15;
  private final static int STAGE_04 = 19;
  private final static int STAGE_05 = 23;
  private final static int STAGE_06 = 26;

  private final static String ICON_SECOND_COLOR = "#123462";
  private final static String ALMOST_WHITE = "#fff3f3";
  private final static String FONT_FILE = "african.ttf";
  private final static int JLABEL_TEXT_BORDER_WIDTH = 4;

  public SplashScreen(final URL imageURL) {
    try {
      this.timer = new Timer(LABEL_UPDATE_DELAY, taskPerformer);
      this.timer.start();

      // ------------------------------------------------ //
      this.frame = new JFrame();
      this.frame.setUndecorated(true);
      this.frame.setAlwaysOnTop(true);
      // transparent background
      this.frame.setBackground(new Color(0, 0, 0, 0));
      this.frame.setUndecorated(true);
      this.frame.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
      // ------------------------------------------------ //

      // ------------------------------------------------ //
      ImageIcon imageIcon = new ImageIcon(imageURL);
      JLabel jLabelWithIcon = new JLabel(imageIcon);
      jLabelWithIcon.setToolTipText("Leave me alone, I'm loading.");
      // ------------------------------------------------ //

      // ------------------------------------------------ //
      jLabelWithText = new JLabel(".", SwingConstants.CENTER);
      jLabelWithText.setForeground(Color.decode(ALMOST_WHITE));
      jLabelWithText.setBackground(Color.black);
      jLabelWithText.setOpaque(true);
      Border blackline = BorderFactory.createLineBorder(Color.decode(ICON_SECOND_COLOR),
          JLABEL_TEXT_BORDER_WIDTH);
      jLabelWithText.setBorder(blackline);
      // ------------------------------------------------ //

      // ------------------------------------------------ //
      // taken from http://www.fontspace.com/allen-r-walden/african
      // --> "Commercial use allowed!"
      // ------------------------------------------------ //
      InputStream is = this.getClass().getResourceAsStream(FONT_FILE);
      if (is == null) {
        throw new NullPointerException("Font file not fount: " + FONT_FILE);
      }
      try {
        Font fontBase = Font.createFont(Font.TRUETYPE_FONT, is);
        Font font = fontBase.deriveFont(Font.PLAIN, 14);
        jLabelWithText.setFont(font);
      } catch (Exception e) {
        e.printStackTrace();
      }
      // ------------------------------------------------ //

      // ------------------------------------------------ //
      this.frame.getContentPane().add(jLabelWithIcon, BorderLayout.CENTER);
      this.frame.getContentPane().add(jLabelWithText, BorderLayout.SOUTH);
      // ------------------------------------------------ //

      Dimension d = new Dimension(150, 150);
      frame.setPreferredSize(d);
      frame.setMinimumSize(d);
      frame.setMaximumSize(d);

      // ------------------------------------------------ //

      final URL iconPath = GUIController.class.getResource(Icon.STAGE_ICON.getFileName());
      final ImageIcon img = new ImageIcon(iconPath);
      this.frame.setIconImage(img.getImage());

      this.makeFrameDraggable(this.frame);
      this.frame.setLocationRelativeTo(null);
      this.frame.pack();
      try {
        Config.inst();
        return;
      } catch (Exception e) {
      }
      this.frame.setVisible(true);
    } catch (Exception e) {
    }
  }

  int counter = 0;
  int timeout = 0;

  ActionListener taskPerformer = new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent ae) {
      try {
        if (frame != null && jLabelWithText != null && frame.isVisible()) {
          if (counter < STAGE_01) {
            jLabelWithText.setText("Load config"
                + StringUtils.repeat(".", counter - THREE_DOTS_COUNT));
          } else if (counter < STAGE_02) {
            jLabelWithText.setText("Init server" + StringUtils.repeat(".", counter - STAGE_01));
          } else if (counter < STAGE_03) {
            jLabelWithText.setText("Init gui" + StringUtils.repeat(".", counter - STAGE_02));
          } else if (counter < STAGE_04) {
            jLabelWithText.setText("Find files" + StringUtils.repeat(".", counter - STAGE_03));
          } else if (counter < STAGE_05) {
            jLabelWithText.setText("Almost done" + StringUtils.repeat(".", counter - STAGE_04));
          } else if (counter < STAGE_06) {
            jLabelWithText
                .setText(StringUtils.repeat("x", counter - (STAGE_04 + THREE_DOTS_COUNT)));
          }
          counter++;
        }
        timeout++;
        if (timeout > 50) {
          close();
        }
      } catch (Exception e) {
      }
    }
  };

  /**
   *
   *
   * @param frame
   */
  private void makeFrameDraggable(final JFrame frame) {
    frame.addMouseListener(new MouseListener() {

      @Override
      public void mouseReleased(MouseEvent e) {
        mousePosition = null;
      }

      @Override
      public void mousePressed(MouseEvent e) {
        mousePosition = e.getPoint();
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        // TODO Auto-generated method stub
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub
      }

      @Override
      public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub
      }
    });

    frame.addMouseMotionListener(new MouseMotionListener() {
      public void mouseMoved(MouseEvent e) {
      }

      public void mouseDragged(MouseEvent e) {
        Point position = e.getLocationOnScreen();
        frame.setLocation(position.x - mousePosition.x, position.y - mousePosition.y);
      }
    });

    frame.addMouseMotionListener(new MouseMotionAdapter() {
      public void mouseDragged(MouseEvent e) {
        Point position = e.getLocationOnScreen();
        frame.setLocation(position.x - mousePosition.x, position.y - mousePosition.y);
      }
    });
  }

  /**
   *
   *
   */
  public void close() {
    if (this.frame != null) {
      this.frame.dispose();
      if (this.timer.isRunning()) {
        this.timer.stop();
      }
      this.frame = null;
      this.timer = null;
    }
  }

  // public static void main(String args[]) {
  // SwingUtilities.invokeLater(new Runnable() {
  // @Override
  // public void run() {
  // try {
  // new SplashScreen(new
  // URL("file:/home/foo/a-ocraptor/img/OCRaptorIconSmall.png"));
  // } catch (MalformedURLException e) {
  // // TODO Auto-generated catch block
  // e.printStackTrace();
  // }
  // }
  // });
  // }
}
