/* Copyright 2025 RTX BBN Technologies */
import "./HashDisplay.css";

const HASH_MAX_DISPLAY_LEN = 15;

export const HashDisplay = ({hash, onCopied} : {hash : string, onCopied : (msg : string) => void}) => {
    const showHashPrefix = (hash : string) => {
        return (hash.length < HASH_MAX_DISPLAY_LEN) ? hash : hash.substring(0, HASH_MAX_DISPLAY_LEN) + "...";
    }

    const copyToClipboard = () => {
        navigator.clipboard.writeText(hash);
        onCopied("Hash copied to clipboard");
    }

    return (
        <div className="margin-left">
            <div className="form-field">
                Hash
            </div>
            <button type="button" className="model-hash" onClick={copyToClipboard}>
                {showHashPrefix(hash)}
            </button>
        </div>
    );
}

export default HashDisplay;