let signalingServer
const remoteVideo = document.getElementById('remoteVideo');

let peerConnection;
let downTime;
let xInitial;
let yInitial;
let xCurrent;
let yCurrent;
let longPressSent = false
let longClickTimeoutId

const configuration = {
    iceServers: [
        {
            urls: 'stun:stun.l.google.com:19302'
        }
    ]
};

remoteVideo.addEventListener('mousedown', (e) => {
    downTime = Date.now()
    xInitial = e.offsetX
    yInitial = e.offsetY
    longClickTimeoutId = setTimeout(longPress, 1000)
})

remoteVideo.addEventListener('mousemove', (e) => {
    xCurrent = e.offsetX
    yCurrent = e.offsetY
})

remoteVideo.addEventListener('mouseup', (e) => {
    console.log('mouse mouseup');
    if(longPressSent === false) {
        clearTimeout(longClickTimeoutId)
        const diff = Date.now() - downTime;
        if(diff < 100 || (Math.abs(xInitial - e.offsetX) < 10 && Math.abs(yInitial - e.offsetY) < 10)) {
            console.log("Mouse click")
            signalingServer.send(JSON.stringify({type: "mouseEvent", event: { type: "click", x: e.offsetX, y: e.offsetY }}))
        }else{
            console.log("Mouse gesture")
            signalingServer.send(JSON.stringify({type: "mouseEvent", event: { type: "gesture", xInitial: xInitial, yInitial: yInitial, xFinal: e.offsetX, yFinal: e.offsetY, time: diff }}))
        }
    }

    longPressSent = false
    xInitial = -1
    yInitial = -1
    downTime = -1
})

remoteVideo.addEventListener("contextmenu", (e) => {
    e.preventDefault();
    signalingServer.send(JSON.stringify({type: "mouseEvent", event: { type: "rightClick" }}))
    return false
}, false)

function longPress(){
    //check if mouse was moved after mouse down event for swipe gesture
    if(Math.abs(xInitial - xCurrent) < 50 && Math.abs(yInitial - yCurrent) < 50) {
        longPressSent = true
        console.log("Mouse long click")
        signalingServer.send(JSON.stringify({type: "mouseEvent", event: { type: "longClick", x: xInitial, y: yInitial }}))
    }
}

function connect(){
    signalingServer = new WebSocket("ws://localhost:8888")

    signalingServer.onopen = function() {
        console.log('Connected to signal server');
        signalingServer.send(JSON.stringify({type: "connectRequest" }))
    }

    signalingServer.onmessage = function(message) {
        console.log(`Received message from signal server: ${message.data}`);
        const data = JSON.parse(message.data);
        console.log(`Type: ${data.type}`)

        if(!peerConnection){
            createPeerConnection()
        }

        switch(data.type){
            case 'offer':
                const sdp2 = JSON.parse(data.sdp);
                // console.log(`Received session description: ${sdp2.description}`);
                peerConnection.setRemoteDescription(new RTCSessionDescription({
                    type: sdp2.type.toLowerCase(),
                    sdp: sdp2.description
                })).then(function() {
                    return peerConnection.createAnswer({
                        offerToReceiveVideo: true,
                    });
                }).then(function(answer) {
                    return peerConnection.setLocalDescription(answer);
                }).then(function() {
                    console.log("Sending answer")
                    signalingServer.send(JSON.stringify({type: "answer", sdp: peerConnection.localDescription }));
                }).catch(function(error) {
                    console.error('Error handling offer', error);
                });
                break;
            case 'answer':
                const sdp = JSON.parse(data.sdp);
                // console.log(`Received session description: ${sdp.description}`);
                peerConnection.setRemoteDescription(new RTCSessionDescription({
                    type: sdp.type.toLowerCase(),
                    sdp: sdp.description
                })).catch(function(error) {
                    console.error('Error setting remote description', error);
                });
                break;
            case 'ice-candidate':
                const iceSdp = JSON.parse(data.candidate);
                peerConnection.addIceCandidate(iceSdp).catch(function(error) {
                    console.error('Error adding received ICE candidate', error);
                });
                break;
            case 'connectConfirm':
                //message from remote device to connect to it via webrtc
                console.log("Connection confirmed, starting webrtc flow")
                // remoteVideo.width = data.width
                // remoteVideo.height = data.height
                createOffer();
                break;
        }
    }

    signalingServer.onclose = function(event){
        console.log(`Connection closed ${event.reason}`)
        closeVideoCall();
        // setTimeout(function() {
        //     connect();
        // }, 10000);
    }

    signalingServer.onerror = function(event){
        signalingServer.close();
    }
}

function createPeerConnection(){
    peerConnection = new RTCPeerConnection(configuration);

    peerConnection.onicecandidate = (event) => {
        console.log("New ice candidate")
        if (event.candidate) {
            signalingServer.send(JSON.stringify({ type: "ice-candidate", candidate: event.candidate, sdpMLineIndex: event.candidate.sdpMLineIndex, sdpMid: event.candidate.sdpMid, sdp: event.candidate.candidate}));
        }
    };

    peerConnection.ontrack = (event) => {
        console.log(`Streams: ${event.streams.length}`)
        remoteVideo.srcObject = event.streams[0];

        if(remoteVideo.srcObject){
            remoteVideo.srcObject.addEventListener("removetrack", (event) => {
                const stream = remoteVideo.srcObject
                const trackList = stream.getTracks()
                if(trackList.length === 0){
                    closeVideoCall()
                }
            })
        }

    };

    peerConnection.onnegotiationneeded = (event) => {
        console.log("Renegotiation needed")
        createOffer()
    }
}

function createOffer(){
    peerConnection.createOffer({
        offerToReceiveVideo: true,
    }).then(function(offer) {
        console.log(`Creating offer`)
        return peerConnection.setLocalDescription(offer);
    }).then(function() {
        signalingServer.send(JSON.stringify({ type: "offer", sdp: peerConnection.localDescription }));
    }).catch(function(error) {
        console.error('Error creating an offer', error);
    });
}

function closeVideoCall() {
    if (peerConnection) {
        peerConnection.ontrack = null;
        peerConnection.onremovetrack = null;
        peerConnection.onremovestream = null;
        peerConnection.onicecandidate = null;
        peerConnection.oniceconnectionstatechange = null;
        peerConnection.onsignalingstatechange = null;
        peerConnection.onicegatheringstatechange = null;
        peerConnection.onnegotiationneeded = null;

        if (remoteVideo.srcObject) {
            remoteVideo.srcObject.getTracks().forEach((track) => track.stop());
        }

        peerConnection.close();
        peerConnection = null;
    }

    // remoteVideo.removeAttribute("src");
    // remoteVideo.removeAttribute("srcObject");
}

connect();