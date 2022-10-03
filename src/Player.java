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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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
    private Song currentSong;
    private int currentFrame;
    private int idmusicaatual;

    private int playerpaused = 1;
    private final ArrayList<Song> songToPlay = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final ReentrantLock lock2 = new ReentrantLock();
    private final Condition lockCondition = lock2.newCondition();
    private Thread threadPlayer;

    Runnable playerRunnable = () -> {

        try {
            currentSong = songToPlay.get(idmusicaatual);
            this.device = FactoryRegistry.systemRegistry().createAudioDevice();
            this.device.open(this.decoder = new Decoder());
            this.bitstream = new Bitstream(currentSong.getBufferedInputStream());

            playerpaused = 0;
            currentFrame = 0;

            while (playNextFrame()) {
                lock2.lock();
                try {
                    if (playerpaused == 1) {
                        lockCondition.await();
                    }
                    window.setPlayingSongInfo(currentSong.getTitle(), currentSong.getAlbum(), currentSong.getArtist());
                    window.setTime(currentFrame * (int) currentSong.getMsPerFrame(), (int) currentSong.getMsLength());
                    currentFrame++;

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    lock2.unlock();
                }
            }
        } catch (JavaLayerException | FileNotFoundException e) {
            e.printStackTrace();
        }
    };

    private final ActionListener buttonListenerPlayNow = e -> {
        new Thread(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                {
                    try {
                        String id = window.getSelectedSong();
                        for (int i = 0; i < songToPlay.size(); i++){
                            if (id.equals(songToPlay.get(i).getUuid())) {
                                idmusicaatual = i;
                                threadPlayer = new Thread(playerRunnable);

                                playerpaused = 0;

                                window.setEnabledPlayPauseButton(true);
                                window.setPlayPauseButtonIcon(playerpaused);

                                threadPlayer.start();
                            }
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            }
        }).start();

    };
    private final ActionListener buttonListenerRemove = e -> {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    lock.lock();
                    String id = window.getSelectedSong();
                    // verificar se uma musica esta sendo tocada
                    // se sim chamar função stop();
                    for (int i = 0; i<songToPlay.size();i++) {
                        if (id.equals(songToPlay.get(i).getUuid())){
                            /*
                            if (i == idmusicaatual){
                                stop();
                            }
                            */
                            songToPlay.remove(i);
                            break;
                        }
                    }

                    window.setQueueList(getDisplayInfo());
                } finally {
                    lock.unlock();
                }
            }
        }).start();
    };
    private final ActionListener buttonListenerAddSong = e -> {
        new Thread(new Runnable() {
            public void run() {
                try {
                    lock.lock();
                    Song newSong = window.openFileChooser();
                    songToPlay.add(newSong);
                    window.setQueueList(getDisplayInfo());

                } catch (IOException | BitstreamException | UnsupportedTagException | InvalidDataException dq) {
                    System.out.println("Erro");
                } finally {
                    lock.unlock();
                }
            }
        }).start();
    };
    private final ActionListener buttonListenerPlayPause = e -> {
        System.out.println(playerpaused);
        if (playerpaused == 1) {
            playerpaused = 0;
            lock2.lock();
            try {
                lockCondition.signalAll();
            } finally {
                lock2.unlock();
            }
            window.setPlayPauseButtonIcon(playerpaused);
        } else {
            playerpaused = 1;
            window.setPlayPauseButtonIcon(playerpaused);
        }
    };
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

    String windowTitle = "yey";

    public Player() {
        EventQueue.invokeLater(() -> window = new PlayerWindow(
                windowTitle,
                getDisplayInfo(),
                buttonListenerPlayNow,
                buttonListenerRemove,
                buttonListenerAddSong,
                buttonListenerPlayPause
                //////////////////////////////
                /*
                buttonListenerShuffle,
                buttonListenerPrevious,
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

    private String[][] getDisplayInfo() {
        String[][] arrayAuxiliar = new String[songToPlay.size()][];

        for (int i=0; i < songToPlay.size(); i++) {
            arrayAuxiliar[i] = songToPlay.get(i).getDisplayInfo();
        }

        return arrayAuxiliar;
    }
    //</editor-fold>
}
