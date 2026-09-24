package net.iqaddons.mod.nanovg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Reads all diagnostic switches without initializing any NanoVG/GL object. */
public final class IqNanoVgConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger("IQ-NanoVG");
    private static final String RAW_MODE = System.getProperty(IqNanoVgMode.PROPERTY);
    private static final IqNanoVgMode MODE = IqNanoVgMode.parse(RAW_MODE);
    private static final boolean TRACE_BLUR = Boolean.parseBoolean(System.getProperty("iq.nanovg.trace.blur", "true"));
    private static final boolean TRACE_STENCIL_STROKES = Boolean.parseBoolean(
            System.getProperty("iq.nanovg.trace.stencilStrokes", "true")
    );

    private IqNanoVgConfiguration() {
    }

    public static void logSelectedMode() {
        if (!IqNanoVgMode.isValid(RAW_MODE)) {
            LOGGER.warn("Invalid -D{}={} value; using full", IqNanoVgMode.PROPERTY, RAW_MODE);
        }
        LOGGER.info("IQ NanoVG diagnostic mode: {} (-D{}={})", MODE.name().toLowerCase(),
                IqNanoVgMode.PROPERTY, MODE.name().toLowerCase());
        if (MODE.tracesGl()) {
            LOGGER.info("IQ NanoVG trace options: blur={} stencilStrokes={} " +
                            "(override with -Diq.nanovg.trace.blur and -Diq.nanovg.trace.stencilStrokes)",
                    TRACE_BLUR, TRACE_STENCIL_STROKES);
        }
    }

    public static IqNanoVgMode mode() {
        return MODE;
    }

    public static boolean traceBlurEnabled() {
        return !MODE.tracesGl() || TRACE_BLUR;
    }

    public static boolean traceStencilStrokesEnabled() {
        return !MODE.tracesGl() || TRACE_STENCIL_STROKES;
    }
}
