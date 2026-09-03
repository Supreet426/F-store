/**
 * Utility functions for Web Admin Portal
 */

/**
 * Escapes HTML characters in a string to prevent XSS injection.
 * @param {any} str Input value to sanitize
 * @returns {string} HTML-escaped string
 */
export function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    const stringified = String(str);
    return stringified
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

/**
 * Utility function to convert tabular data into a CSV file and trigger download in browser.
 * @param {string} filename Name of the generated CSV file
 * @param {Array<string>} headers Column header labels
 * @param {Array<Array<any>>} rows Matrix of row values
 */
export function exportToCsv(filename, headers, rows) {
    if (!rows || !rows.length) {
        if (window.showToast) window.showToast("No data available to export", "error");
        return;
    }

    const csvRows = [];
    csvRows.push(headers.map(h => `"${String(h).replace(/"/g, '""')}"`).join(','));

    for (const row of rows) {
        csvRows.push(row.map(val => `"${String(val ?? '').replace(/"/g, '""')}"`).join(','));
    }

    const csvContent = csvRows.join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);

    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute('download', filename);
    link.style.visibility = 'hidden';

    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    if (window.showToast) window.showToast(`Exported ${rows.length} rows to ${filename}`, "success");
}
