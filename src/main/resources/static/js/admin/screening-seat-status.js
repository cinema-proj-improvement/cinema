// 좌석 현황 모달 열기
async function openSeatModal(screeningId, subtitleText) {
    const res = await fetch(`/admin/api/screenings/${screeningId}/seats`);
    const data = await res.json();

    console.log('seat api response =', data);

    renderSeatSummary(data.summary);
    renderSeats(data.seats);

    console.log('summary =', data.summary);
    console.log('seats =', data.seats);

    document.getElementById('seatModalSubtitle').innerText = subtitleText;
    document.getElementById('seatStatusModal').classList.remove('hidden');
}

// 모달 닫기
function closeSeatModal() {
    document.getElementById('seatStatusModal').classList.add('hidden');
}

// 요약 렌더링
function renderSeatSummary(s) {
    qs('sumTotal').innerText = s.total;
    qs('sumConfirmed').innerText = s.confirmed;
    qs('sumHold').innerText = s.hold;
    qs('sumAvailable').innerText = s.available;
}

// 좌석 렌더링
function renderSeats(seats) {
    const grid = qs('seatGrid');
    grid.innerHTML = '';

    const grouped = {};
    seats.forEach(seat => {
        grouped[seat.rowNo] ??= [];
        grouped[seat.rowNo].push(seat);
    });

    Object.keys(grouped)
        .sort((a, b) => a - b)
        .forEach(rowNo => {
            const row = document.createElement('div');
            row.className = 'seat-row';

            const label = document.createElement('span');
            label.className = 'row-label';
            label.innerText = String.fromCharCode(64 + Number(rowNo));
            row.appendChild(label);

            grouped[rowNo]
                .sort((a, b) => a.colNo - b.colNo)
                .forEach(seat => {
                    const el = document.createElement('div');
                    el.className = `seat ${seat.status.toLowerCase()}`;
                    el.innerText = seat.colNo;
                    row.appendChild(el);
                });

            grid.appendChild(row);
        });
}
