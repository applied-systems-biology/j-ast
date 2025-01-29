export function formatSeconds(seconds : number) {
  const days = Math.floor(seconds / (24 * 3600));
  seconds %= 24 * 3600;
  const hours = Math.floor(seconds / 3600);
  seconds %= 3600;
  const minutes = Math.floor(seconds / 60);
  seconds %= 60;

  const parts = [];
  if (days > 0) parts.push(`${days} day${days > 1 ? 's' : ''}`);
  if (hours > 0) parts.push(`${hours} hour${hours > 1 ? 's' : ''}`);
  if (minutes > 0) parts.push(`${minutes} minute${minutes > 1 ? 's' : ''}`);
  if (seconds > 0) parts.push(`${seconds} second${seconds > 1 ? 's' : ''}`);

  return parts.join(', ').replace(/,([^,]*)$/, ' and$1');
}

export function splitByDelimiters(input: string, delimiters: string): string[] {
  // Escape special regex characters in the delimiters
  const escapedDelimiters = delimiters.replace(/[-/\\^$*+?.()|[\]{}]/g, '\\$&');
  // Create a regular expression to split by any of the delimiters
  const delimiterRegex = new RegExp(`[${escapedDelimiters}]`, 'g');
  // Split the input string using the regex
  return input.split(delimiterRegex);
}

export function formatNumberPlural(number: number, singular : string): string {
  if(number == 1) {
    return number + " " + singular;
  }
  else {
    return number + " " + singular + "s";
  }
}

export function formatExpirationTime(seconds : number) {
  const expirationDate = new Date(Date.now() + seconds * 1000);
  return expirationDate.toLocaleString(); // Adjusts to the user's local timezone
}

export function formatFileSize(bytes: number): string {
  const units = ["bytes", "KB", "MB", "GB", "TB"];
  if (bytes === 0) return "0 bytes";

  const exponent = Math.floor(Math.log(bytes) / Math.log(1024));
  const size = bytes / Math.pow(1024, exponent);

  return `${size.toFixed(2)} ${units[exponent]}`;
}

export function sortPathsByHierarchy(paths: Iterable<string>): string[] {
  // Convert the input to an array (works for Set, Array, or any iterable)
  const pathsArray = Array.from(paths);

  // Sort the paths by hierarchy and alphabetically
  return pathsArray.sort((a, b) => {
    const depthA = a.split('/').length;
    const depthB = b.split('/').length;

    if (depthA === depthB) {
      // If depths are equal, sort alphabetically
      return a.localeCompare(b);
    }
    // Otherwise, sort by depth
    return depthA - depthB;
  });
}

export function makeFilesystemCompatible(input: string): string {
  // Define a regex to match forbidden characters
  const forbiddenChars = /[<>:"/\\|?*\x00]/g;
  // Replace forbidden characters with an underscore or other safe character
  let cleanString = input.replace(forbiddenChars, '_');
  // Trim whitespace from the beginning and end
  cleanString = cleanString.trim();
  // Ensure the length does not exceed common filename limits (255 characters)
  if (cleanString.length > 255) {
    cleanString = cleanString.substring(0, 255);
  }
  return cleanString;
}
