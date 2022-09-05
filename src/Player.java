import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.UnsupportedTagException;
import javazoom.jl.decoder.*;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;
import support.PlayerWindow;
import support.Song;

import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList; // adcionado

public class Player {
    /**
     * The MPEG audio bitstream.
     */
    private Bitstream bitstream;
    /**
     * The MPEG audio decoder.
     */
    private Decoder decoder;
    /**
     * The AudioDevice where audio samples are written to.
     */
    private AudioDevice device;

    private PlayerWindow window;

    private final ArrayList<Song> songToPlay = new ArrayList<>();

    private int currentFrame = 0;



    private final ActionListener buttonListenerPlayNow = e -> {};
    private final ActionListener buttonListenerRemove = e -> {};
    private final ActionListener buttonListenerAddSong = e -> {
        ////////////////////////////////////////////////
        try {
            Song newSong = window.openFileChooser();
            songToPlay.add(newSong);
            window.setQueueList(getDisplayInfo());

        } catch (IOException | BitstreamException | UnsupportedTagException | InvalidDataException dq) {
            System.out.println("Erro");
        }
        ////////////////////////////////////////////////

    };
    private final ActionListener buttonListenerPlayPause = e -> {};
    private final ActionListener buttonListenerStop = e -> {};
    private final ActionListener buttonListenerNext = e -> {};
    private final ActionListener buttonListenerPrevious = e -> {};
    private final ActionListener buttonListenerShuffle = e -> {};
    private final ActionListener buttonListenerLoop = e -> {};
    private final MouseInputAdapter scrubberMouseInputAdapter = new MouseInputAdapter() {
        @Override
        public void mouseReleased(MouseEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {
        }

        @Override
        public void mouseDragged(MouseEvent e) {
        }
    };

    String windowTitle = "JPlayer";

    public Player() {
        EventQueue.invokeLater(() -> window = new PlayerWindow(
                windowTitle,
                buttonListenerAddSong
                //////////////////////////////
                /*
                getDisplayInfo(),
                buttonListenerPlayNow,
                buttonListenerRemove,
                buttonListenerShuffle,
                buttonListenerPrevious,
                buttonListenerPlayPause,
                buttonListenerStop,
                buttonListenerNext,
                buttonListenerLoop,
                scrubberMouseInputAdapter */)
                /////////////////////////////

        );
    }

    //<editor-fold desc="Essential">

    /**
     * @return False if there are no more frames to play.
     */
    private boolean playNextFrame() throws JavaLayerException {
        // TODO Is this thread safe?
        if (device != null) {
            Header h = bitstream.readFrame();
            if (h == null) return false;

            SampleBuffer output = (SampleBuffer) decoder.decodeFrame(h, bitstream);
            device.write(output.getBuffer(), 0, output.getBufferLength());
            bitstream.closeFrame();
        }
        return true;
    }

    /**
     * @return False if there are no more frames to skip.
     */
    private boolean skipNextFrame() throws BitstreamException {
        // TODO Is this thread safe?
        Header h = bitstream.readFrame();
        if (h == null) return false;
        bitstream.closeFrame();
        currentFrame++;
        return true;
    }

    /**
     * Skips bitstream to the target frame if the new frame is higher than the current one.
     *
     * @param newFrame Frame to skip to.
     * @throws BitstreamException Generic Bitstream exception.
     */
    private void skipToFrame(int newFrame) throws BitstreamException {
        // TODO Is this thread safe?
        if (newFrame > currentFrame) {
            int framesToSkip = newFrame - currentFrame;
            boolean condition = true;
            while (framesToSkip-- > 0 && condition) condition = skipNextFrame();
        }
    }
    //</editor-fold>

    ///////////////////////////////////////////////////////////
    private String[][] getDisplayInfo() {
        String[][] arrayAuxiliar = new String[songToPlay.size()][];

        for (int i=0; i < songToPlay.size(); i++) {
            arrayAuxiliar[i] = songToPlay.get(i).getDisplayInfo();
        }

        return arrayAuxiliar;
    }
    ///////////////////////////////////////////////////////////

}
