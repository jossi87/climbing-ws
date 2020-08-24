package com.buldreinfo.jersey.jaxb.thumbnailcreator;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteSink;

import java.io.IOException;
import java.io.OutputStream;

class SingleByteSink extends ByteSink implements AutoCloseable {
    private OutputStream outputStream;
    private boolean consumed;
    private boolean closed;

    /**
     * Construct a new single byte stream.
     * @param outputStream the output stream to return once.
     */
    public SingleByteSink(OutputStream outputStream) {
        this.outputStream = Preconditions.checkNotNull(outputStream, "outputStream cannot be NULL");
    }

    /**
     * Determine if the single byte sink has been consumed.
     * @return TRUE if it has, FALSE otherwise.
     */
    public boolean isConsumed() {
        return consumed;
    }

    @Override
    public OutputStream openStream() throws IOException {
        if (closed) {
            throw new IllegalStateException("Byte sink has already been closed.");
        }
        if (consumed) {
            throw new IllegalStateException("This byte sink has already been consumed.");
        }
        consumed = true;
        return outputStream;
    }

    @Override
    public void close() throws Exception {
        // Only close the stream if we haven't consumed it
        if (!consumed && !closed) {
            outputStream.close();
            closed = true;
        }
    }
}
