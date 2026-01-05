/* Copyright 2025 RTX BBN Technologies */

export const TextFormField = ({id, placeholder, label, value, onChange} : 
    {id : string, placeholder : string, label : string, value : any, onChange : (e : any) => void}) => {
    return (
        <div className="flex make-row margin">
            <div className="flex make-column width-max">
                <label htmlFor={id}>
                    <span className="form-field">{label}</span>
                </label>
                <input 
                    placeholder={placeholder} 
                    id={id}
                    className="flex make-row margin-rop margin-bottom dark-form-field" 
                    type="text" 
                    value={value}
                    onChange={(e) => onChange(e.target.value)}
                    required
                />
            </div>
        </div>
    );
}

export default TextFormField;