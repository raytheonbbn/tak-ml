/* Copyright 2025 RTX BBN Technologies */

import "./ModelsTable.css";
import { type ModelDescriptor } from "../../types/Types";
import { createColumnHelper, flexRender, getCoreRowModel, getPaginationRowModel, useReactTable } from "@tanstack/react-table";
import ActionsBar from "../ActionsBar/ActionsBar";
import { ModelNameCell } from "../ModelNameCell/ModelNameCell";
import RunLocationPills from "../RunLocationPills/RunLocationPills";

export const ModelsTable = ({data, /*setData,*/ onUpdate, onDelete, onSuccess} : 
    {data : ModelDescriptor[], /*setData : Dispatch<SetStateAction<ModelDescriptor[]>>,*/ onUpdate : (modelDescriptor : ModelDescriptor, file : File|null) => void, onDelete : (modelDescriptor : ModelDescriptor) => void, onSuccess : (msg : string) => void}) => {
    
    // const [data, setData] = useState<ModelDescriptor[]>(() => testModelDescriptors);
    // const [pagination, setPagination] = useState({
    //     pageIndex: 0,  // initial page index
    //     pageSize: 10,  // default page size
    // });

    const columnHelper = createColumnHelper<ModelDescriptor>();

    const columns = [
        columnHelper.accessor('name', {
            header: () => <span>Name</span>,
            footer: props => props.column.id,
            cell: props => <ModelNameCell modelDescriptor={props.row.original} />
        }),
        columnHelper.accessor('modelType', {
            header: () => <span>Type</span>,
            footer: props => props.column.id
        }),
        columnHelper.accessor('framework', {
            header: () => <span>Framework</span>,
            footer: props => props.column.id
        }),
        // columnHelper.accessor('accuracy', {
        //     header: () => <span>Accuracy</span>,
        //     footer: props => props.column.id,
        //     cell: props => <ModelAccuracyDisplay modelDescriptor={props.row.original} />
        // }),
        columnHelper.accessor('numInferences', {
            header: () => <span>No. Inferences</span>,
            footer: props => props.column.id,
            cell: props => <p>{props.cell.getValue()}</p>
        }),
        columnHelper.accessor('sizeMegabytes', {
            header: () => <span>Size</span>,
            footer: props => props.column.id,
            cell: props => <p>{props.cell.getValue()} MB</p>
        }),
        columnHelper.accessor('runOnServer', {
            header: () => <span>Runs On</span>,
            footer: props => props.column.id,
            cell: props => <RunLocationPills runLocation={props.cell.getValue()}/>
        }),
        // columnHelper.accessor('status', {
        //     header: () => <span>Status</span>,
        //     footer: props => props.column.id,
        //     cell: props => <p><b>{props.cell.getValue() === ModelStatus.ACTIVE ? "active" : "archived"}</b></p>
        // }),
        columnHelper.display({
            id: 'actions',
            header: () => <span>Actions</span>,
            cell: props => <ActionsBar row={props.row.original} onUpdate={onUpdate} onDelete={() => onDelete(props.row.original)} onSuccess={onSuccess} />,
        })
    ];

    const table = useReactTable({ 
        columns, 
        data, 
        getCoreRowModel: getCoreRowModel(),
        getPaginationRowModel: getPaginationRowModel(),
        // state: {
        //     pagination,
        // },
    });
    
    if (data === null || data === undefined || data.length === 0) {
        return (
            <div className="flex flex-center">
                <p className="empty-table-display"><i>No models to display</i></p>
            </div>
        )
    }

    return (
        <table className="table">
            <thead>
                {table.getHeaderGroups().map((hg) => (
                    <tr key={hg.id}>
                        {hg.headers.map((header) => (
                            <th key={header.id}>
                                {flexRender(header.column.columnDef.header, header.getContext())}
                            </th>
                        ))}
                    </tr>
                ))}
            </thead>
            <tbody>
                {table.getRowModel().rows.map((row) => (
                    <tr key={row.id}>
                        {row.getVisibleCells().map((cell) => (
                            <td key={cell.id} className="table-cell">
                                {flexRender(cell.column.columnDef.cell, cell.getContext())}
                            </td>
                        ))}
                    </tr>
                ))}
            </tbody>
        </table>
    )
};

export default ModelsTable;