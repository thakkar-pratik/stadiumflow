document.addEventListener("DOMContentLoaded", () => {
    
    // REGISTER PWA SERVICE WORKER
    if ('serviceWorker' in navigator) {
        window.addEventListener('load', () => {
            navigator.serviceWorker.register('/sw.js')
                .then(reg => console.log('✅ PWA Service Worker Registered!'))
                .catch(err => console.log('❌ PWA Service Worker Failed', err));
        });
    }

    // UNIFIED NAVIGATION LOGIC (Syncs Sidebar + Bottom Nav)
    const navButtons = document.querySelectorAll(".nav-item");
    const views = document.querySelectorAll(".view");

    navButtons.forEach(btn => {
        btn.addEventListener("click", () => {
            const targetId = btn.getAttribute("data-target");
            
            // 1. Update all nav buttons across the app
            navButtons.forEach(b => {
                if (b.getAttribute("data-target") === targetId) {
                    b.classList.add("active");
                } else {
                    b.classList.remove("active");
                }
            });

            // 2. Switch the active view
            views.forEach(v => {
                if (v.id === targetId) {
                    v.classList.add("active");
                } else {
                    v.classList.remove("active");
                }
            });
        });
    });

    // NETWORK TOGGLE LOGIC (Unified Mobile & Desktop)
    let isOnline = true;
    const networkToggleBtn = document.getElementById("network-toggle");
    const networkToggleDesktop = document.getElementById("network-toggle-desktop");
    const networkLabel = document.getElementById("network-label");
    const meshPromo = document.getElementById("mesh-promo");
    const checkoutBtn = document.getElementById("checkout-main-btn");
    const checkoutWarning = document.getElementById("checkout-network-warning");

    function updateNetworkUI(status) {
        isOnline = status;
        const stateClass = isOnline ? "online" : "offline";
        const labelText = isOnline ? "5G Online" : "BLE Mesh";

        // Update Sync: Both buttons must stay in sync
        [networkToggleBtn, networkToggleDesktop].forEach(btn => {
            if (btn) {
                btn.className = `network-status ${stateClass}`;
                const innerLabel = btn.querySelector("span:not(.status-dot)");
                if (innerLabel) innerLabel.innerText = labelText;
                if (btn.id === "network-toggle-desktop") {
                    btn.innerHTML = `<span class="status-dot"></span> ${labelText}`;
                }
            }
        });

        if (isOnline) {
            networkLabel.innerText = "5G Online";
            meshPromo.className = "promo-card";
            meshPromo.innerHTML = "<h3>Game Day Update</h3><p>Welcome to Section 112. The network is strong, but BLE Mesh is on standby.</p>";
            checkoutBtn.className = "checkout-btn";
            checkoutBtn.innerText = "Checkout";
            checkoutWarning.classList.add("hidden");
        } else {
            networkLabel.innerText = "BLE Mesh";
            meshPromo.className = "promo-card offline";
            meshPromo.innerHTML = "<h3>⚠️ Cellular Connection Lost</h3><p>You are now connected via the peer-to-peer BLE Mesh network. Orders will be securely bounced via nearby phones.</p>";
            checkoutBtn.className = "checkout-btn offline-mode";
            checkoutBtn.innerText = "Checkout (Offline Pay)";
            checkoutWarning.classList.remove("hidden");
        }
    }

    if (networkToggleBtn) networkToggleBtn.addEventListener("click", () => updateNetworkUI(!isOnline));
    if (networkToggleDesktop) networkToggleDesktop.addEventListener("click", () => updateNetworkUI(!isOnline));

    // DEBOUNCE UTILITY FOR EFFICIENCY
    function debounce(func, wait) {
        let timeout;
        return function(...args) {
            clearTimeout(timeout);
            timeout = setTimeout(() => func.apply(this, args), wait);
        };
    }

    // EVENT STREAM CONNECTION
    const eventSource = new EventSource("/api/iot/stream");
    eventSource.onopen = () => console.log("✅ Stream Connected!");
    
    // Wrap heavy DOM updates in debouncer (Max 2 updates per second)
    const debouncedUpdates = debounce((data) => {
        updateHomeDashboard(data);
        updateMapScreen(data);
        updateYieldPricing(data);
        executeSmartReroute(data);
    }, 500);

    eventSource.onmessage = function(event) {
        if (!isOnline) {
            console.log("Network disconnected: Ignoring live SSE updates (Simulation)");
            return;
        }
        try {
            const data = JSON.parse(event.data);
            debouncedUpdates(data);
        } catch (e) {}
    };

    function updateHomeDashboard(zones) {
        const grid = document.getElementById("wait-times-grid");
        grid.innerHTML = "";
        zones.forEach(zone => {
            let statusClass = "status-low";
            if (zone.waitTime > 10) statusClass = "status-med";
            if (zone.waitTime > 20) statusClass = "status-high";
            const card = document.createElement("div");
            card.className = "card";
            card.innerHTML = `<div class="zone-name"><span class="status-indicator ${statusClass}"></span>${zone.name}</div><div class="wait-time">${zone.waitTime} <span class="wait-label">mins</span></div>`;
            grid.appendChild(card);
        });
    }

    function updateMapScreen(zones) {
        zones.forEach(zone => {
            const mapElement = document.getElementById("map-zone-" + zone.id);
            if (mapElement) {
                mapElement.classList.remove("low", "med", "high");
                let severity = "low";
                if (zone.waitTime > 10) severity = "med";
                if (zone.waitTime > 20) severity = "high";
                mapElement.classList.add(severity);
            }
        });
    }

    function updateYieldPricing(zones) {
        processYield("Section_112", 112, 8.00, zones);
        processYield("Section_120", 120, 12.00, zones);
    }

    // Capture dynamic price globally so we can send it to the backend DB
    let current112Price = 8.00; 

    function processYield(zoneId, domId, basePrice, zones) {
        const zoneData = zones.find(z => z.id === zoneId);
        if (!zoneData) return;
        const badge = document.getElementById("badge-" + domId);
        const priceTxt = document.getElementById("price-" + domId);
        const oldPriceTxt = document.getElementById("old-price-" + domId);
        
        badge.innerText = `Wait: ${zoneData.waitTime}m`;
        let computedPrice = basePrice;
        
        if (zoneData.waitTime < 5) {
            computedPrice = basePrice * 0.75;
            priceTxt.innerText = "$" + computedPrice.toFixed(2);
            priceTxt.classList.add("discount"); oldPriceTxt.classList.remove("hidden");
            badge.style.background = "rgba(0, 230, 118, 0.2)"; badge.style.color = "var(--status-green)";
        } else if (zoneData.waitTime > 15) {
            computedPrice = basePrice;
            priceTxt.innerText = "$" + computedPrice.toFixed(2);
            priceTxt.classList.remove("discount"); oldPriceTxt.classList.add("hidden");
            badge.style.background = "rgba(255, 23, 68, 0.2)"; badge.style.color = "var(--status-red)";
        } else {
            computedPrice = basePrice;
            priceTxt.innerText = "$" + computedPrice.toFixed(2);
            priceTxt.classList.remove("discount"); oldPriceTxt.classList.add("hidden");
            badge.style.background = "rgba(254, 234, 0, 0.2)"; badge.style.color = "var(--status-yellow)";
        }
        
        if (domId === 112) current112Price = computedPrice;
    }

    function executeSmartReroute(zones) {
        const alertBox = document.getElementById("smart-alert");
        const alertText = document.getElementById("alert-message");
        const routePath = document.getElementById("live-route-path");

        if (!zones || zones.length === 0) return;

        // Find the zone with the lowest wait time that is either a Gate or Section
        let bestZone = null;
        for (const z of zones) {
            if (!bestZone || z.waitTime < bestZone.waitTime) {
                bestZone = z;
            }
        }

        if (!bestZone) return;

        const coords = {
            "Gate_A": { x: 50, y: 15 },
            "Gate_C": { x: 50, y: 85 },
            "Section_112": { x: 85, y: 50 },
            "Section_120": { x: 15, y: 50 }
        };

        const myCoords = { x: 50, y: 60 };
        const dest = coords[bestZone.id];
        
        if (!dest) return;

        // Draw an organic curve to the destination
        const midX = (myCoords.x + dest.x) / 2 + (myCoords.y - dest.y) * 0.2;
        const midY = (myCoords.y + dest.y) / 2 + (dest.x - myCoords.x) * 0.2;
        const route = `M ${myCoords.x} ${myCoords.y} Q ${midX} ${midY}, ${dest.x} ${dest.y}`;

        routePath.setAttribute("d", route);
        
        // Show an alert dynamically if we reroute
        if (bestZone.waitTime < 10) {
            alertText.innerText = `Smart Route Active: ${bestZone.name} is nearest and relatively empty (${bestZone.waitTime}m).`;
            alertBox.classList.remove("hidden");
        } else {
            alertText.innerText = `All areas busy. Best option is ${bestZone.name} (${bestZone.waitTime}m).`;
            alertBox.classList.remove("hidden");
        }
    }

    const openArBtn = document.getElementById("open-ar-btn");
    const closeArBtn = document.getElementById("close-ar-btn");
    const arOverlay = document.getElementById("ar-overlay");
    const webcamFeed = document.getElementById("webcam-feed");
    let localStream = null;

    openArBtn.addEventListener("click", async () => {
        arOverlay.classList.remove("hidden");
        
        const scanDiv = document.createElement("div");
        scanDiv.id = "ar-scanning";
        scanDiv.className = "ar-scanning";
        scanDiv.innerHTML = "<span>🛰️ SCANNING WORLD...</span>";
        arOverlay.appendChild(scanDiv);
        
        const constraints = [
            { video: { facingMode: { exact: "environment" } } }, // Back Cam
            { video: true } // Fallback to any cam (Front/Laptop)
        ];

        for (let constraint of constraints) {
            try {
                localStream = await navigator.mediaDevices.getUserMedia(constraint);
                webcamFeed.srcObject = localStream;
                webcamFeed.play();
                break; 
            } catch (e) {
                console.warn("Back camera not available, trying fallback...");
            }
        }

        if (!localStream) {
            alert("No camera found. Using simulated positioning.");
        }

        setTimeout(() => {
            const scan = document.getElementById("ar-scanning");
            if (scan) scan.remove();
        }, 1500);
    });

    closeArBtn.addEventListener("click", () => {
        arOverlay.classList.add("hidden");
        const scan = document.getElementById("ar-scanning");
        if (scan) scan.remove();
        if (localStream) {
            localStream.getTracks().forEach(track => track.stop());
            localStream = null;
        }
    });

    // REST API BACKEND INTEGRATION
    checkoutBtn.addEventListener("click", async () => {
        
        const orderPayload = {
            zoneId: "Section_112",
            itemMenu: "Mega Hotdog",
            paidPrice: current112Price,
            offlineMesh: !isOnline
        };

        if (isOnline) {
            // FIRE REAL REST API POST REQUEST TO JAVA BACKEND DB
            checkoutBtn.innerText = "Processing...";
            try {
                const response = await fetch("/api/orders", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(orderPayload)
                });
                if (response.ok) {
                    alert("Order successfully written to Spring Boot H2 Database over 5G!");
                } else {
                    alert("Order failed. Ensure Java is running!");
                }
            } catch (err) {
                console.error(err);
                alert("Could not reach Backend API");
            } finally {
                checkoutBtn.innerText = "Checkout";
            }
        } else {
            // OFFLINE BLE MESH SIMULATION
            document.getElementById("ble-mesh-overlay").classList.remove("hidden");
            
            for (let i=1; i<=6; i++) {
                document.getElementById(`log-${i}`).classList.add("hidden");
            }
            document.getElementById("peer-node").classList.add("hidden");
            document.getElementById("qr-result").classList.add("hidden");

            setTimeout(() => document.getElementById("log-1").classList.remove("hidden"), 500);
            setTimeout(() => document.getElementById("log-2").classList.remove("hidden"), 1500);
            setTimeout(() => {
                document.getElementById("log-3").classList.remove("hidden");
                document.getElementById("peer-node").classList.remove("hidden");
            }, 3000);
            setTimeout(() => document.getElementById("log-4").classList.remove("hidden"), 4000);
            setTimeout(() => {
                document.getElementById("log-5").classList.remove("hidden");
                document.getElementById("peer-node").style.transform = "scale(1.5)";
                document.getElementById("peer-node").style.background = "var(--status-green)";
            }, 5500);
            setTimeout(() => {
                document.getElementById("log-6").classList.remove("hidden");
                // Here we would normally sync the `orderPayload` once we regain network connection!
                document.getElementById("qr-result").classList.remove("hidden");
            }, 7000);
        }
    });

});

// AI CHAT LOGIC
function toggleChat() {
    const chatWin = document.getElementById("ai-chat-window");
    chatWin.classList.toggle("hidden");
}

async function sendChatMessage() {
    const input = document.getElementById("chat-input");
    const container = document.getElementById("chat-messages");
    const query = input.value.trim();
    if (!query) return;

    // Append User Msg
    appendMsg(query, "user");
    input.value = "";

    try {
        const response = await fetch("/api/ai/ask", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ query: query })
        });
        const data = await response.json();
        appendMsg(data.response, "bot");
    } catch (err) {
        appendMsg("I'm having trouble connecting to my central brain. Please check your backend!", "bot");
    }
}

function appendMsg(text, type) {
    const container = document.getElementById("chat-messages");
    const div = document.createElement("div");
    div.className = `msg ${type}`;
    div.innerText = text;
    container.appendChild(div);
    container.scrollTop = container.scrollHeight;
}

document.getElementById("send-chat-btn").addEventListener("click", sendChatMessage);
document.getElementById("chat-input").addEventListener("keypress", (e) => {
    if (e.key === "Enter") sendChatMessage();
});

function dismissAlert() { document.getElementById("smart-alert").classList.add("hidden"); }

// VIEW MODE TOGGLE (Judge Helper)
const toggleDesktop = document.getElementById("toggle-view-desktop");
const toggleMobile = document.getElementById("toggle-view-mobile");

function togglePerspective() {
    document.body.classList.toggle("force-mobile");
    const isForced = document.body.classList.contains("force-mobile");
    if (toggleDesktop) toggleDesktop.innerText = isForced ? "🖥️ Switch to Desktop" : "📱 Toggle Mobile View";
    if (toggleMobile) toggleMobile.innerText = isForced ? "🖥️ Full View" : "🖥️ Exit Mobile View";
}

if (toggleDesktop) toggleDesktop.addEventListener("click", togglePerspective);
if (toggleMobile) toggleMobile.addEventListener("click", togglePerspective);
