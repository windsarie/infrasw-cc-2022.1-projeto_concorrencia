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
    private int novoframe;
    private boolean next = false;
    private boolean previous = false;
    private boolean Parar = false;
    private final ArrayList<Song> songToPlay = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final ReentrantLock lock2 = new ReentrantLock();
    private final Condition lockCondition = lock2.newCondition();
    private Thread threadPlayer;
    Runnable playerRunnable = () -> {
        try {
            window.setEnabledStopButton(true);
            window.setEnabledScrubber(true);
            while (idmusicaatual < songToPlay.size()) {
                window.setEnabledNextButton(idmusicaatual == songToPlay.size() - 1);
                window.setEnabledPreviousButton(idmusicaatual != 0);

                currentSong = songToPlay.get(idmusicaatual);
                this.device = FactoryRegistry.systemRegistry().createAudioDevice();
                this.device.open(this.decoder = new Decoder());
                this.bitstream = new Bitstream(currentSong.getBufferedInputStream());

                playerpaused = 0;
                currentFrame = 0;

                while (playNextFrame()) {
                    lock2.lock();
                    try {
                        if (Parar) {
                            Parar = false;
                            window.resetMiniPlayer();
                            threadPlayer.stop();
                        }
                        if (playerpaused == 1) {
                            lockCondition.await();
                        }
                        window.setPlayingSongInfo(currentSong.getTitle(), currentSong.getAlbum(), currentSong.getArtist());
                        window.setTime(currentFrame * (int) currentSong.getMsPerFrame(), (int) currentSong.getMsLength());
                        currentFrame++;
                        if(next) {
                            next = false;
                            idmusicaatual++;
                            break;
                        }
                        if(previous) {
                            previous = false;
                            idmusicaatual--;
                            break;
                        }

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        lock2.unlock();
                    }
                }
                idmusicaatual++;
            }
            window.resetMiniPlayer();
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
                    for (int i = 0; i<songToPlay.size();i++) {
                        if (id.equals(songToPlay.get(i).getUuid())){
                            if (idmusicaatual == i) {
                                Parar = true;
                            }
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
    private final ActionListener buttonListenerStop = e -> {
        Parar = true;
    };
    private final ActionListener buttonListenerNext = e -> {
        next = true;
    };
    private final ActionListener buttonListenerPrevious = e -> {
        previous = true;
    };
    private final ActionListener buttonListenerShuffle = e -> {};
    private final ActionListener buttonListenerLoop = e -> {};
    private final MouseInputAdapter scrubberMouseInputAdapter = new MouseInputAdapter() {
        @Override
        public void mouseReleased(MouseEvent e) {
            try {
                if (novoframe < currentFrame) {
                    bitstream.close();
                    device = FactoryRegistry.systemRegistry().createAudioDevice();
                    device.open(decoder = new Decoder());
                    bitstream = new Bitstream(currentSong.getBufferedInputStream());
                    currentFrame = 0;
                }
                skipToFrame(novoframe);
            } catch (FileNotFoundException | JavaLayerException ex) {
                throw new RuntimeException(ex);
            }
            playerpaused = 1;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            playerpaused = 0;
            novoframe = (window.getScrubberValue()/(int)currentSong.getMsPerFrame());
            window.setTime(currentFrame * (int) currentSong.getMsPerFrame(), (int) currentSong.getMsLength());
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            playerpaused = 0;
            novoframe = (window.getScrubberValue()/(int)currentSong.getMsPerFrame());
            window.setTime(currentFrame * (int) currentSong.getMsPerFrame(), (int) currentSong.getMsLength());
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
                buttonListenerPlayPause,
                buttonListenerStop,
                buttonListenerNext,
                buttonListenerPrevious,
                scrubberMouseInputAdapter
                /*
                buttonListenerShuffle,

                

                buttonListenerLoop,
                 */)
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
