import SwiftUI
import AVFoundation

/// Живое распознавание штрихкода через камеру (AVFoundation).
/// Поток найденных кодов отдаётся через `onCode`.
final class ScannerViewController: UIViewController, AVCaptureMetadataOutputObjectsDelegate {

    var onCode: ((String) -> Void)?

    private let session = AVCaptureSession()
    private var preview: AVCaptureVideoPreviewLayer?
    private let sessionQueue = DispatchQueue(label: "barcode.session")
    private(set) var isConfigured = false

    private static let symbologies: [AVMetadataObject.ObjectType] = [
        .ean13, .ean8, .upce, .code128, .code39, .code93, .itf14,
        .interleaved2of5, .qr, .dataMatrix, .pdf417, .aztec
    ]

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black
        sessionQueue.async { [weak self] in self?.configure() }
    }

    private func configure() {
        session.beginConfiguration()
        session.sessionPreset = .high

        guard let device = AVCaptureDevice.default(for: .video),
              let input = try? AVCaptureDeviceInput(device: device),
              session.canAddInput(input) else {
            session.commitConfiguration()
            return
        }
        session.addInput(input)

        let output = AVCaptureMetadataOutput()
        guard session.canAddOutput(output) else {
            session.commitConfiguration()
            return
        }
        session.addOutput(output)
        output.setMetadataObjectsDelegate(self, queue: DispatchQueue.main)
        output.metadataObjectTypes = Self.symbologies.filter {
            output.availableMetadataObjectTypes.contains($0)
        }
        session.commitConfiguration()

        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            let layer = AVCaptureVideoPreviewLayer(session: self.session)
            layer.videoGravity = .resizeAspectFill
            layer.frame = self.view.bounds
            self.view.layer.insertSublayer(layer, at: 0)
            self.preview = layer
            self.applyPortraitRotation()
            self.isConfigured = true
        }
    }

    private func applyPortraitRotation() {
        guard let connection = preview?.connection else { return }
        let angle: CGFloat = 90 // портрет
        if connection.isVideoRotationAngleSupported(angle) {
            connection.videoRotationAngle = angle
        }
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        preview?.frame = view.bounds
        applyPortraitRotation()
    }

    func start() {
        sessionQueue.async { [weak self] in
            guard let self, !self.session.isRunning else { return }
            self.session.startRunning()
        }
    }

    func stop() {
        sessionQueue.async { [weak self] in
            guard let self, self.session.isRunning else { return }
            self.session.stopRunning()
        }
    }

    func setTorch(_ on: Bool) {
        guard let device = AVCaptureDevice.default(for: .video), device.hasTorch else { return }
        try? device.lockForConfiguration()
        device.torchMode = on ? .on : .off
        device.unlockForConfiguration()
    }

    func metadataOutput(_ output: AVCaptureMetadataOutput,
                        didOutput metadataObjects: [AVMetadataObject],
                        from connection: AVCaptureConnection) {
        guard let object = metadataObjects.first as? AVMetadataMachineReadableCodeObject,
              let value = object.stringValue, !value.isEmpty else { return }
        onCode?(value)
    }
}

struct BarcodeScannerView: UIViewControllerRepresentable {
    @Binding var isActive: Bool
    var torchOn: Bool
    var onCode: (String) -> Void

    func makeUIViewController(context: Context) -> ScannerViewController {
        let vc = ScannerViewController()
        vc.onCode = onCode
        return vc
    }

    func updateUIViewController(_ vc: ScannerViewController, context: Context) {
        vc.onCode = onCode
        if isActive { vc.start() } else { vc.stop() }
        vc.setTorch(torchOn && isActive)
    }
}

/// Статус доступа к камере для ветвления UI.
enum CameraAccess {
    case authorized, denied, undetermined, unavailable

    static var current: CameraAccess {
        #if targetEnvironment(simulator)
        return .unavailable
        #else
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:           return .authorized
        case .denied, .restricted:  return .denied
        case .notDetermined:        return .undetermined
        @unknown default:           return .denied
        }
        #endif
    }

    static func request() async -> Bool {
        await AVCaptureDevice.requestAccess(for: .video)
    }
}
