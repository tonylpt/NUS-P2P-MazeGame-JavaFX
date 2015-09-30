import java.io.Serializable;

/**
 * @author lpthanh
 */
public abstract class GameParams {

    private final HostPort hostPort;

    protected GameParams(HostPort hostPort) {
        this.hostPort = hostPort;
    }

    public HostPort getHostPort() {
        return hostPort;
    }

    public abstract boolean isPrimary();

    /**
     * Implementation for Primary server.
     */
    public static class PrimaryParams extends GameParams {

        private final int boardSize;

        private final int treasureCount;

        public PrimaryParams(HostPort hostPort, int boardSize, int treasureCount) {
            super(hostPort);
            this.boardSize = boardSize;
            this.treasureCount = treasureCount;
        }

        /**
         * @return whether the player should be started as the primary server
         */
        @Override
        public boolean isPrimary() {
            return true;
        }

        public int getBoardSize() {
            return boardSize;
        }

        public int getTreasureCount() {
            return treasureCount;
        }

        public static PrimaryParams parse(String param) {
            try {
                // split by ',' and parse the string
                String[] primaryArgs = param.split(",");
                String hostPortString = primaryArgs[0];
                HostPort hostPort = HostPort.parse(hostPortString);
                int boardSize = Integer.parseInt(primaryArgs[1]);
                int treasureCount = Integer.parseInt(primaryArgs[2]);
                return new PrimaryParams(hostPort, boardSize, treasureCount);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid format. Expecting: host:port,board-size,treasure-count.", e);
            }
        }
    }

    /**
     * Implementation for normal (non-primary) player.
     */
    public static class NonPrimaryParams extends GameParams {

        public NonPrimaryParams(HostPort hostPort) {
            super(hostPort);
        }

        @Override
        public boolean isPrimary() {
            return false;
        }

        public static NonPrimaryParams parse(String param) {
            try {
                HostPort hostPort = HostPort.parse(param);
                return new NonPrimaryParams(hostPort);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid format. Expecting: host:port.", e);
            }
        }
    }

    /**
     * This class represents a host:port pair.
     */
    public static class HostPort implements Serializable {

        private final String host;

        private final int port;

        public HostPort(String host, int port) {
            this.port = port;
            this.host = host;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        /**
         * Parse a string in one of the following forms into a HostPort instance:
         * host:port_number
         * :port_number
         * port_number
         */
        public static HostPort parse(String hostPortString) {
            String[] hostParts = hostPortString.split(":");
            String hostName;
            String portStr;
            int port;

            if (hostParts.length == 1) {
                hostName = null;
                portStr = hostParts[0];
            } else if (hostParts.length == 2) {
                hostName = hostParts[0];
                portStr = hostParts[1];
            } else {
                throw new IllegalArgumentException("Invalid format. Expect host:port.");
            }

            // Parsing the port from string to int
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid format. Port must be an integer.");
            }

            return new HostPort(hostName, port);
        }
    }
}
