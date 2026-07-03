uniffi::setup_scaffolding!();

/// Returns the BLAKE3 hash of the file at `path`, as a lowercase hex string.
/// Used by the duplicate finder (Ring 3, F2) and, later, bulk-copy integrity checks.
#[uniffi::export]
pub fn hash_file(path: String) -> Result<String, RustCoreError> {
    let mut hasher = blake3::Hasher::new();
    let mut file = std::fs::File::open(&path).map_err(|e| RustCoreError::Io(e.to_string()))?;
    std::io::copy(&mut file, &mut hasher).map_err(|e| RustCoreError::Io(e.to_string()))?;
    Ok(hasher.finalize().to_hex().to_string())
}

#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum RustCoreError {
    #[error("I/O error: {0}")]
    Io(String),
}
