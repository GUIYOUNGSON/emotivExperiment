hLine = plot(nan);
port = 6789;
t = tcpip('localhost', port, 'NetworkRole', 'server');
fopen(t);
for n = 1:2000
    data = fread(t, 14, 'double');
    data;
    set(hLine, 'YData', data);
    drawnow;
end
