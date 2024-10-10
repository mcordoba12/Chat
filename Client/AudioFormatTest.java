import javax.sound.sampled.*;

public class AudioFormatTest {
    public static void main(String[] args) {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixers) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            Line.Info[] lineInfos = mixer.getTargetLineInfo();
            for (Line.Info lineInfo : lineInfos) {
                if (lineInfo instanceof DataLine.Info) {
                    DataLine.Info dataLineInfo = (DataLine.Info) lineInfo;
                    AudioFormat[] supportedFormats = dataLineInfo.getFormats();
                    System.out.println("Supported formats for mixer " + mixerInfo.getName() + ":");
                    for (AudioFormat format : supportedFormats) {
                        System.out.println(format);
                    }
                }
            }
        }
    }
}
