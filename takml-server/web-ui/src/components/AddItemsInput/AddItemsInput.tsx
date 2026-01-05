/* Copyright 2025 RTX BBN Technologies */
import { useState } from "react";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faPlusCircle } from "@fortawesome/free-solid-svg-icons";
import RemovablePill from "../RemovablePill/RemovablePill.tsx";
import "./AddItemsInput.css";

export const AddItemsInput = ({id, header, initialItemsList, placeholder, onItemsListChanged} : {id : string, header : string, initialItemsList : string[], placeholder : string, onItemsListChanged : (newItemsList : string[]) => void}) => {
    const [items, setItems] = useState<string[]>((initialItemsList) ? initialItemsList : []);
    const [itemToAdd, setItemToAdd] = useState<string>("");

    const deleteItem = (itemToDelete : string) => {
        console.log("Delete button pressed for item: ", itemToDelete);
        const newItems = items.length === 0 ? [] : items.filter(function(tag) {
            return tag !== itemToDelete;
        });
        setItems(newItems);
        onItemsListChanged(newItems);
    }

    const addItem = (itemToAdd : string) => {
        if (itemToAdd === null || itemToAdd === "") {
            console.warn("Not adding empty item");
            return;
        }
        console.log("Adding item: ", itemToAdd);
        var _items = Array.from(items);
        if (!items.includes(itemToAdd)) {
            _items.push(itemToAdd);
            setItems(_items);
            onItemsListChanged(_items);
        }
        setItemToAdd("");
    }

    return (
        <div className="flex make-row margin">
            <div className="flex make-column width-max">
                <label className="flex make-row" htmlFor={id}>
                    <span className="form-field">{header}</span>
                </label>
                <div className="flex make-row flex-center-items">
                    <div className="flex make-column width-thirty-percent margin-right pill-input-min-width">
                        <input
                            placeholder={placeholder}
                            id={id}
                            className="margin-top margin-bottom dark-form-field" 
                            type="text"
                            value={itemToAdd}
                            onChange={(e) => setItemToAdd(e.target.value)}
                        />
                    </div>
                    <div className="flex margin-top margin-bottom make-column width-ten-percent">
                        <button className="add-pill-button" 
                            onClick={e => {
                                // Don't submit the form
                                e.preventDefault();
                                addItem(itemToAdd);
                                setItemToAdd("");
                            }}>
                            <FontAwesomeIcon icon={faPlusCircle} />
                        </button>
                    </div>
                </div>
                <div className="flex make-row flex-wrap">
                    {items.map((item, i) => {
                        return <RemovablePill
                                    name={item}
                                    key={"pill-" + i} 
                                    val={item}
                                    onDelete={(item) => {
                                        deleteItem(item);
                                    }} 
                                />
                    })}
                </div>
            </div>
        </div>
    )
}