import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.UnsupportedTagException;
import javazoom.jl.decoder.*;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;
import support.PlayerWindow;
import support.Song;

import java.awt.*;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
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

    private final ArrayList<Song> songToPlay = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private boolean stop = false;
    private boolean playerPaused = true;


    private Thread threadPlayer;

    private int currentFrame = 0;

    private Song currentSong;
    private Object await;
    private final Runnable playerRunnable = () -> {
        String id = window.getSelectedSong();
        // procurar no array musica com id
        try {
            this.device = FactoryRegistry.systemRegistry().createAudioDevice();
        } catch (JavaLayerException e) {
            throw new RuntimeException(e);
        }
        try {
            this.device.open(this.decoder = new Decoder());
        } catch (JavaLayerException e) {
            throw new RuntimeException(e);
        }
        try {
            this.bitstream = new Bitstream(currentSong.getBufferedInputStream());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        stop = false;
        // loop
        for (int i = 0; i < tamanholista; i++); {
            if (boolean stop = true){
                break();
            } else{
                await();
            }
        }
        // verificar se o boolean de parar mudou, se sim break
        // verificar se o boolean de pausar mudou, se sim await
        playNextFrame
        // atualizar tempo na janela
        int frame;
        int msPerFrame;
        int numFrames;
        EventQueue.invokeAndWait(() -> window.setTime(frame * msPerFrame, numFrames * msPerFrame));

    };

    private final ActionListener buttonListenerPlayNow = e -> {
        threadPlayer = new Thread(playerRunnable);
    };
    private final ActionListener buttonListenerRemove = e -> {
        // remover do array
        // verificar se a musica esta tocando. se sim, parar reproducao
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    lock.lock();

                    int tamanholista = songToPlay.size();

                    for (int i = 0; i < tamanholista; i++) {
                        if (filePath.equals(songToPlay.get(i).getFilePath())) {

                            if (indiceMusicaAtual == i) {
                                indiceMusicaAtual -= 1;
                                next();
                                if (indiceMusicaAtual == songToPlay.size() - 1) { //Impedir que ele pule um indice assim que uma música é removida
                                    stop();
                                }
                            }
                            if (i < indiceMusicaAtual) {
                                indiceMusicaAtual -= 1;
                                if (indiceMusicaAtual == 0) {
                                    window.setEnabledPreviousButton(false);
                                }

                            }
                        }

                        songToPlay.remove(i);

                        break;
                    }
                } finally {
                    lock.unlock();
                }
            }
        });

        private final ActionListener buttonListenerAddSong = e -> {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        lock.lock();
                        Song newSong = window.openFileChooser();

                        class HelloRunnable implements Runnable {

                            public void run() {
                                System.out.println("Hello from a thread!");
                            }

                            public static void main(String[] args) {
                                (new Thread(new HelloRunnable())).start();
                            }
                        }
                        songToPlay.add(newSong);
                        EventQueue.invokeLater
                        String[][] info = getDisplayInfo();
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
            new Thread(new Runnable() {
                @Override
                public void run() {
                    lock.lock();
                    try {

                        for (int i = 0; i < songToPlay.size(); i++) {
                            if (musicStart.equals(songToPlay.get(i).getFilePath())) {
                                indiceMusicaAtual = i;
                                threadPlayMusic = new Thread(new RunnablePlay());
                                playerPaused = false;
                                window.updatePlayPauseButtonIcon(playerPaused);
                                window.setEnabledPlayPauseButton(!playerPaused);
                                threadPlayMusic.start();

                            }

                        }
                        playerPaused = true;
                        window.updatePlayPauseButtonIcon(playerPaused);

                    } finally {
                        lock.unlock();
                    }

                }
            }).start();

        }

    private void run() {
        try {
            lock.lock();
            condition.signalAll();


        } finally {
            lock.unlock();
        }
        // mudar variavel
        stop = true

    }

    ;
    private final ActionListener buttonListenerStop = e -> {
        // mudar variavel
        stop = true;
        //EventQueue.invokeAndWait(() -> window.resetMiniPlayer());
    };

    //private final ActionListener buttonListenerNext = e -> {};
    //private final ActionListener buttonListenerPrevious = e -> {};
    //private final ActionListener buttonListenerShuffle = e -> {};
    //private final ActionListener buttonListenerLoop = e -> {};
    //private final MouseInputAdapter scrubberMouseInputAdapter = new MouseInputAdapter() {
    //@Override
    //public void mouseReleased(MouseEvent e) {}

    //@Override
    //public void mousePressed(MouseEvent e) {}

    //@Override
    //public void mouseDragged(MouseEvent e) {}
    //};

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
    private String[][] playNextFrame() throws JavaLayerException {
        // TODO Is this thread safe?
        lock.lock();
        try {
            if (device != null) {
                Header h = bitstream.readFrame();
                if (h == null) return false;

                SampleBuffer output = (SampleBuffer) decoder.decodeFrame(h, bitstream);
                device.write(output.getBuffer(), 0, output.getBufferLength());
                bitstream.closeFrame();
            }
        } finally {
            lock.unlock();
        }
        return true;


        /**
         * @return False if there are no more frames to skip.
         */
        private boolean skipNextFrame () throws BitstreamException {
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
        private void skipToFrame ( int newFrame) throws BitstreamException {
            // TODO Is this thread safe?
            if (newFrame > currentFrame) {
                int framesToSkip = newFrame - currentFrame;
                boolean condition = true;
                while (framesToSkip-- > 0 && condition) condition = skipNextFrame();
            }
        }
        //</editor-fold>

        ///////////////////////////////////////////////////////////
        private String[][] getDisplayInfo () {
            String[][] arrayAuxiliar = new String[songToPlay.size()][];

            for (int i = 0; i < songToPlay.size(); i++) {
                arrayAuxiliar[i] = songToPlay.get(i).getDisplayInfo();
            }

            return arrayAuxiliar;
        }
        ///////////////////////////////////////////////////////////

    }
}
