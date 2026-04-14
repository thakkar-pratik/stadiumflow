document.addEventListener("DOMContentLoaded", () => {
    
    // 1. REGISTER PWA SERVICE WORKER
    if ('serviceWorker' in navigator) {
        window.addEventListener('load', () => {
            navigator.serviceWorker.register('/sw.js')
                .then(reg => console.log('✅ PWA Service Worker Registered!'))
                .catch(err => console.log('❌ PWA Service Worker Failed', err));
        });
    }

    // 2. UNIFIED NAVIGATION LOGIC
    const navButtons = document.querySelectorAll(".nav-item");
    const views = document.querySelectorAll(".view");

    navButtons.forEach(btn => {
        btn.addEventListener("click", () => {
            const targetId = btn.getAttribute("data-target");
            navButtons.forEach(b => {
                if (b.getAttribute("data-target") === targetId) {
                    b.classList.add("active");
                } else {
                    b.classList.remove("active");
                }
            });
            views.forEach(v => {
                if (v.id === targetId) {
                    v.classList.add("active");
                } else {
                    v.classList.remove("active");
                }
            });
        });
    });

    // 3. NETWORK & OFFLINE LOGIC
    let isOnline = true;
    const networkToggleBtn = document.getElementById("network-toggle");
    const networkToggleDesktop = document.getElementById("network-toggle-desktop");
    const networkLabel = document.getElementById("network-label");
    const meshPromo = document.getElementById("mesh-promo");
    const checkoutBtn = document.getElementById("checkout-main-btn");
    const checkoutWarning = document.getElementById("checkout-network-warning");

    async function updateNetworkUI(status) {
        // PERMISSION SIMULATION: Request Bluetooth access before switching to Mesh
        if (!status && isOnline) {
            const permission = confirm(" stadiumpulse-live.web.app wants to turn on Bluetooth. Allow device scanning for Mesh Network?");
            if (!permission) return;
            alert("Bluetooth Enabled. Switching to BLE Mesh Protocol...");
        }

        isOnline = status;
        const stateClass = isOnline ? "online" : "offline";
        const labelText = isOnline ? "5G Online" : "BLE Mesh";

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
            meshPromo.innerHTML = "<h3>Coldplay Experience Update</h3><p>Welcome to the North Stand. The 5G network is strong, but BLE Mesh is ready for the crowd peaks.</p>";
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

    // 4. PERFORMANCE UTILITIES
    function debounce(func, wait) {
        let timeout;
        return function(...args) {
            clearTimeout(timeout);
            timeout = setTimeout(() => func.apply(this, args), wait);
        };
    }

    // 5. HYBRID LIVE TELEMETRY (SSE + POLLING FALLBACK)
    let sseActive = false;
    let fallbackInterval = null;

    function startPollingFallback() {
        if (fallbackInterval) return;
        console.warn("⚠️ SSE Stream lagging/blocked. Switching to Hybrid Polling Failsafe...");
        fallbackInterval = setInterval(async () => {
            if (sseActive) {
                console.log("🔄 SSE recovered. Stopping polling.");
                clearInterval(fallbackInterval);
                fallbackInterval = null;
                return;
            }
            try {
                const response = await fetch("/api/iot/data");
                if (response.ok) {
                    const data = await response.json();
                    debouncedUpdates(data);
                }
            } catch (e) {
                console.error("Polling failed", e);
            }
        }, 5000); // Poll every 5 seconds as a safety net
    }

    const eventSource = new EventSource("/api/iot/stream");
    
    // WATCHDOG: If no data in 7 seconds, start polling
    const watchdog = setTimeout(() => {
        if (!sseActive) startPollingFallback();
    }, 7000);

    eventSource.onopen = () => {
        console.log("✅ Stream Connected!");
    };
    
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
        
        // Mark SSE as healthy
        sseActive = true;
        clearTimeout(watchdog);
        
        try {
            const data = JSON.parse(event.data);
            debouncedUpdates(data);
        } catch (e) {
            // Heartbeats and comments might fail JSON.parse, which is fine
        }
    };

    eventSource.onerror = () => {
        console.warn("❌ SSE Stream Error. Triggering immediate polling fallback.");
        startPollingFallback();
    };

    function updateHomeDashboard(zones) {
        const grid = document.getElementById("wait-times-grid");
        if (!grid) return;
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

    let current112Price = 600; 
    function updateYieldPricing(zones) {
        processYield("Section_112", 112, 600, zones);
        processYield("Section_120", 120, 150, zones);
    }

    function processYield(zoneId, domId, basePrice, zones) {
        const zoneData = zones.find(z => z.id === zoneId);
        if (!zoneData) return;
        const badge = document.getElementById("badge-" + domId);
        const priceTxt = document.getElementById("price-" + domId);
        const oldPriceTxt = document.getElementById("old-price-" + domId);
        
        badge.innerText = `Wait: ${zoneData.waitTime}m`;
        let computedPrice = basePrice;
        
        if (zoneData.waitTime < 5) {
            computedPrice = basePrice * 0.8;
            priceTxt.innerText = "₹" + Math.round(computedPrice);
            priceTxt.classList.add("discount"); if (oldPriceTxt) oldPriceTxt.classList.remove("hidden");
            badge.style.background = "rgba(0, 230, 118, 0.2)"; badge.style.color = "var(--status-green)";
        } else if (zoneData.waitTime > 15) {
            computedPrice = basePrice;
            priceTxt.innerText = "₹" + Math.round(computedPrice);
            priceTxt.classList.remove("discount"); if (oldPriceTxt) oldPriceTxt.classList.add("hidden");
            badge.style.background = "rgba(255, 23, 68, 0.2)"; badge.style.color = "var(--status-red)";
        } else {
            computedPrice = basePrice;
            priceTxt.innerText = "₹" + Math.round(computedPrice);
            priceTxt.classList.remove("discount"); if (oldPriceTxt) oldPriceTxt.classList.add("hidden");
            badge.style.background = "rgba(254, 234, 0, 0.2)"; badge.style.color = "var(--status-yellow)";
        }
        
        if (domId === 112) current112Price = computedPrice;
    }

    function executeSmartReroute(zones) {
        const alertBox = document.getElementById("smart-alert");
        const alertText = document.getElementById("alert-message");
        const routePath = document.getElementById("live-route-path");

        if (!zones || zones.length === 0 || !alertBox || !alertText || !routePath) return;

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

        const midX = (myCoords.x + dest.x) / 2 + (myCoords.y - dest.y) * 0.2;
        const midY = (myCoords.y + dest.y) / 2 + (dest.x - myCoords.x) * 0.2;
        const route = `M ${myCoords.x} ${myCoords.y} Q ${midX} ${midY}, ${dest.x} ${dest.y}`;
        routePath.setAttribute("d", route);
        
        if (bestZone.waitTime < 10) {
            alertText.innerText = `Smart Route Active: ${bestZone.name} is nearest and relatively empty (${bestZone.waitTime}m).`;
            alertBox.classList.remove("hidden");
        } else {
            alertText.innerText = `All areas busy. Best option is ${bestZone.name} (${bestZone.waitTime}m).`;
            alertBox.classList.remove("hidden");
        }
    }

    // 6. AR NAVIGATION LOGIC
    const openArBtn = document.getElementById("open-ar-btn");
    const closeArBtn = document.getElementById("close-ar-btn");
    const arOverlay = document.getElementById("ar-overlay");
    const webcamFeed = document.getElementById("webcam-feed");
    let localStream = null;

    if (openArBtn) {
        openArBtn.addEventListener("click", async () => {
            arOverlay.classList.remove("hidden");
            const scanDiv = document.createElement("div");
            scanDiv.id = "ar-scanning";
            scanDiv.className = "ar-scanning";
            scanDiv.innerHTML = "<span>🛰️ SCANNING WORLD...</span>";
            arOverlay.appendChild(scanDiv);
            
            const constraints = [
                { video: { facingMode: { exact: "environment" } } },
                { video: true }
            ];

            for (let constraint of constraints) {
                try {
                    localStream = await navigator.mediaDevices.getUserMedia(constraint);
                    webcamFeed.srcObject = localStream;
                    webcamFeed.play();
                    break; 
                } catch (e) {
                    console.warn("Camera fallback...");
                }
            }

            setTimeout(() => {
                const scan = document.getElementById("ar-scanning");
                if (scan) scan.remove();
            }, 1500);
        });
    }

    if (closeArBtn) {
        closeArBtn.addEventListener("click", () => {
            arOverlay.classList.add("hidden");
            if (localStream) {
                localStream.getTracks().forEach(track => track.stop());
                localStream = null;
            }
        });
    }

    // 7. RETAIL LOGIC: CART & QUANTITIES
    const cart = { "112": 0, "120": 0 };
    window.updateQty = function(id, change) {
        cart[id] = Math.max(0, cart[id] + change);
        const qtyEl = document.getElementById(`qty-${id}`);
        if (qtyEl) qtyEl.innerText = cart[id];
    };

    function generateVisualBarcode() {
        const container = document.getElementById("barcode-area");
        if (!container) return;
        container.innerHTML = "";
        for (let i = 0; i < 40; i++) {
            const bar = document.createElement("div");
            bar.className = "bar";
            // Randomly vary bar widths for a realistic barcode look
            const width = Math.floor(Math.random() * 4) + 1;
            bar.style.width = `${width}px`;
            container.appendChild(bar);
        }
        const token = "PX-" + Math.floor(100 + Math.random() * 900);
        const tokenEl = document.getElementById("ble-token");
        if (tokenEl) tokenEl.innerText = token;
    }

    if (checkoutBtn) {
        checkoutBtn.addEventListener("click", async () => {
            const totalQty = cart["112"] + cart["120"];
            if (totalQty === 0) {
                alert("Please add at least one item to your cart!");
                return;
            }

            const orderPayload = {
                zoneId: "Section_112",
                items: [
                    { name: "Wankhede Spicy Burger", qty: cart["112"] },
                    { name: "Cosmic Water", qty: cart["120"] }
                ],
                paidPrice: (cart["112"] * 600) + (cart["120"] * 150),
                offlineMesh: !isOnline
            };

            if (isOnline) {
                checkoutBtn.innerText = "Processing...";
                try {
                    const response = await fetch("/api/orders", {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify(orderPayload)
                    });
                    if (response.ok) {
                        alert("Order Successful!");
                    } else {
                        alert("Checkout error. Please try again.");
                    }
                } catch (err) {
                    alert("Could not reach Backend API");
                } finally {
                    checkoutBtn.innerText = "Proceed to Payment";
                }
            } else {
                generateVisualBarcode();
                simulateBleMesh();
            }
        });
    }

    function simulateBleMesh() {
        document.getElementById("ble-mesh-overlay").classList.remove("hidden");
        for (let i=1; i<=6; i++) {
            const el = document.getElementById(`log-${i}`);
            if (el) el.classList.add("hidden");
        }
        const peer = document.getElementById("peer-node");
        const qr = document.getElementById("qr-result");
        if (peer) peer.classList.add("hidden");
        if (qr) qr.classList.add("hidden");

        setTimeout(() => document.getElementById("log-1").classList.remove("hidden"), 500);
        setTimeout(() => document.getElementById("log-2").classList.remove("hidden"), 1500);
        setTimeout(() => {
            document.getElementById("log-3").classList.remove("hidden");
            if (peer) peer.classList.remove("hidden");
        }, 3000);
        setTimeout(() => document.getElementById("log-4").classList.remove("hidden"), 4000);
        setTimeout(() => {
            document.getElementById("log-5").classList.remove("hidden");
            if (peer) {
                peer.style.transform = "scale(1.5)";
                peer.style.background = "var(--status-green)";
            }
        }, 5500);
        setTimeout(() => {
            document.getElementById("log-6").classList.remove("hidden");
            if (qr) qr.classList.remove("hidden");
        }, 7000);
    }

    // 8. GEMINI AI ASSISTANT LOGIC
    const sendChatBtn = document.getElementById("send-chat-btn");
    const chatInput = document.getElementById("chat-input");

    window.toggleChat = function() {
        const chatWin = document.getElementById("ai-chat-window");
        if (chatWin) chatWin.classList.toggle("hidden");
    };

    async function sendChatMessage() {
        if (!chatInput) return;
        const query = chatInput.value.trim();
        if (!query) return;

        appendMsg(query, "user");
        chatInput.value = "";

        try {
            const response = await fetch("/api/ai/ask", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ query: query })
            });
            const data = await response.json();
            appendMsg(data.response, "bot");
        } catch (err) {
            appendMsg("I'm having trouble connecting to my central brain.", "bot");
        }
    }

    function appendMsg(text, type) {
        const container = document.getElementById("chat-messages");
        if (!container) return;
        const div = document.createElement("div");
        div.className = `msg ${type}`;
        div.innerText = text;
        container.appendChild(div);
        container.scrollTop = container.scrollHeight;
    }

    if (sendChatBtn) sendChatBtn.addEventListener("click", sendChatMessage);
    if (chatInput) {
        chatInput.addEventListener("keypress", (e) => {
            if (e.key === "Enter") sendChatMessage();
        });
    }

    window.dismissAlert = function() { 
        const alertNode = document.getElementById("smart-alert");
        if (alertNode) alertNode.classList.add("hidden"); 
    };

    // 9. VIEW MODE TOGGLE (Judge Helper)
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

});
