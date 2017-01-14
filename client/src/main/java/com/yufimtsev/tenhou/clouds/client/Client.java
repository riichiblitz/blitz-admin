package com.yufimtsev.tenhou.clouds.client;

import com.yufimtsev.tenhou.clouds.client.callback.IOnChatMessageReceived;
import com.yufimtsev.tenhou.clouds.client.callback.IOnStateChangedCallback;

import java.io.IOException;

public class Client {

    private UserState state;

    private Internal internalApi;
    private DataReceiver receiver;
    private KeepAliver keepAliver;

    IOnStateChangedCallback pendingCallback;
    IOnStateChangedCallback autoCallback = null;
    IOnStateChangedCallback defaultCallback = state1 -> {
        //System.out.println("STATE CHANGED: " + state1.name());
        if (autoCallback != null) {
            autoCallback.onStateChanged(state1);
        }
    };

    IOnChatMessageReceived autoChatCallback = null;
    IOnChatMessageReceived chatCallback = message -> {
        //System.out.println("Chat message: " + message);
        if (autoChatCallback != null) {
            autoChatCallback.onChatMessageReceived(message);
        }
    };
    private String userName;

    public Client() {
        state = UserState.DISCONNECTED;
    }

    public void connect() {
        connect(null);
    }

    public void setCallback(IOnStateChangedCallback callback) {
        autoCallback = callback;
    }

    public void setChatCallback(IOnChatMessageReceived callback) {
        autoChatCallback = callback;
    }

    public void connect(IOnStateChangedCallback callback) {
        if (state != UserState.DISCONNECTED) {
            throw new IllegalStateException();
        }
        setState(UserState.CONNECTING, callback);
        try {
            internalApi = new Internal();
            receiver = new DataReceiver(internalApi);
            receiver.start();
            setState(UserState.CONNECTED, callback);
        } catch (IOException e) {
            setState(UserState.DISCONNECTED, callback);
        }
    }

    public void disconnect(IOnStateChangedCallback callback) {
/*        if (state == UserState.DISCONNECTED || state == UserState.DISCONNECTING) {
            throw new IllegalStateException();
        }*/
        setState(UserState.DISCONNECTING, callback);
        if (keepAliver != null) {
            keepAliver.disconnect();
        }
        receiver.disconnect();
        internalApi.disconnect();
        internalApi.destroy();
        setState(UserState.DISCONNECTED, callback);
    }

    public void authenticate(String name, IOnStateChangedCallback callback) {
        if (state != UserState.CONNECTED) {
            throw new IllegalStateException();
        }
        userName = name;
        setState(UserState.AUTHETICATING, callback);
        pendingCallback = callback;
        internalApi.helo(name);
    }

    public void changeLobby(String lobby, IOnStateChangedCallback callback) {
        if (state != UserState.IDLE) {
            throw new IllegalStateException();
        }
        setState(UserState.CHANGING_LOBBY, callback);
        keepAliver.disconnect();
        pendingCallback = callback;
        internalApi.lobby(lobby);
    }

    public void changeChampLobby(String lobby, IOnStateChangedCallback callback) {
        if (state != UserState.IDLE) {
            throw new IllegalStateException();
        }
        setState(UserState.CHANGING_LOBBY, callback);
        keepAliver.disconnect();
        pendingCallback = callback;
        internalApi.champLobby(lobby);
    }

    public void who() {
        internalApi.who();
    }

    public void sendMessage(String message) {
        internalApi.chat(message);
    }

    public void getPlayes() {
        internalApi.who();
    }

    private void setState(UserState state, IOnStateChangedCallback callback) {
        //System.out.println(state.name() + " : " + userName);
        this.state = state;
        sendState(callback);
        defaultCallback.onStateChanged(state);
    }

    private void sendState(IOnStateChangedCallback callback) {
        if (callback != null) {
            callback.onStateChanged(state);
        }
    }

    private void parseMessage(String message) {
        //System.out.println("Parsing message: " + message);
        if (message.startsWith("<HELO")) {
            switch (state) {
                case AUTHETICATING:
                    internalApi.auth(internalApi.generateAuthToken(Decoder.parseAuth(message)));
                    break;
                default:
                    throw new IllegalStateException();
            }
        } else if (message.startsWith("<LN")) {
            switch (state) {
                case AUTHETICATING:
                    keepAliver = KeepAliver.startNew(internalApi);
                    IOnStateChangedCallback currentPendingCallback = pendingCallback;
                    pendingCallback = null;
                    setState(UserState.IDLE, currentPendingCallback);
                    break;
                case CHANGING_LOBBY:
                case IDLE:
                    break;
                case JOINING:
                case PLAYING:
                    internalApi.sendMessage(internalApi.getPrxTag(false));
                    break;
                default:
                    throw new IllegalStateException();
            }
        } else if (message.startsWith("<CHAT")) {
            chatCallback.onChatMessageReceived(Decoder.parseChat(message));
            if (message.startsWith("<CHAT LOBBY")) {
                switch (state) {
                    case CHANGING_LOBBY:
                        keepAliver = KeepAliver.startNew(internalApi);
                        setState(UserState.IDLE, pendingCallback);
                        //pendingCallback = null;
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }
        } else if (message.startsWith("<DISCONNECT")) {
            disconnect(pendingCallback);
        }
    }

    private class DataReceiver extends Thread {

        private Internal internal;
        private boolean receiving;

        public DataReceiver(Internal internal) {
            this.internal = internal;
            receiving = true;
        }

        public void disconnect() {
            receiving = false;
        }

        @Override
        public void run() {
            while (receiving) {
                String[] messages = internal.readMultipleMessages();
                if (messages.length > 0) {
                    int a = 0;
                }
                for (String message: messages) {
                    if (message != null && message.length() > 0) {
                        parseMessage(message.trim().toUpperCase());
                    }
                }
                try {
                    sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class KeepAliver extends Thread {
        private Internal internal;
        private boolean working;

        private static KeepAliver instance;

        public static KeepAliver startNew(Internal internal) {
            if (instance != null) {
                instance.disconnect();
            }
            instance = new KeepAliver(internal);
            instance.start();
            return instance;
        }

        public KeepAliver(Internal internal) {
            this.internal = internal;
            working = true;
        }

        public void disconnect() {
            working = false;
        }

        @Override
        public void run() {
            while (working) {
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                internal.keepAlive();

                try {
                    sleep(14000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
