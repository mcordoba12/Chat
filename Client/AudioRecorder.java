import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AudioRecorder {
    private AudioFormat format;
    private TargetDataLine microphone;
    private ByteArrayOutputStream out;
    private boolean recording;

    public AudioRecorder() {
        // Configuración del formato de audio
        format = new AudioFormat(16000, 16, 2, true, true); // 16kHz, 16 bits, estéreo, big endian
    }

    public void startRecording() throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        microphone = (TargetDataLine) AudioSystem.getLine(info);
        microphone.open(format);
        microphone.start();

        out = new ByteArrayOutputStream();
        recording = true;

        Thread recordingThread = new Thread(() -> {
            byte[] buffer = new byte[4096];
            while (recording) {
                int bytesRead = microphone.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        recordingThread.start();
    }

    public byte[] stopRecording() {
        recording = false;
        microphone.stop();
        microphone.close();
        return out.toByteArray();
    }
}
